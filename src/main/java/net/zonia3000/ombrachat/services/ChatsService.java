package net.zonia3000.ombrachat.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import javafx.application.Platform;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.controllers.ChatPageController;
import net.zonia3000.ombrachat.controllers.MainWindowController;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatsService {

    private static final Logger logger = LoggerFactory.getLogger(ChatsService.class);

    private final ConcurrentMap<Integer, List<Long>> chatFolders = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, TdApi.Chat> chats = new ConcurrentHashMap<>();
    private final Object loadChatsLock = new Object();
    private boolean haveFullMainChatList = false;
    private int selectedChatFolder;

    private final Object lock = new Object();
    private TdApi.ChatFolderInfo[] chatFoldersInfo = null;
    private TdApi.Chat selectedChat;

    private final TelegramClientService telegramClientService;
    private final GuiService guiService;

    public ChatsService() {
        this.telegramClientService = ServiceLocator.getService(TelegramClientService.class);
        this.guiService = ServiceLocator.getService(GuiService.class);
    }

    public boolean onResult(TdApi.Object object) {
        if (object instanceof TdApi.UpdateChatFolders update) {
            return handleUpdateChatFolders(update);
        } else if (object instanceof TdApi.UpdateNewChat update) {
            return handleUpdateNewChat(update);
        } else if (object instanceof TdApi.UpdateChatPosition update) {
            return handleUpdateChatPosition(update);
        } else if (object instanceof TdApi.UpdateChatLastMessage update) {
            return handleUpdateChatLastMessage(update);
        } else if (object instanceof TdApi.UpdateChatAddedToList update) {
            return handleUpdateChatAddedToList(update);
        } else if (object instanceof TdApi.UpdateChatReadInbox update) {
            return handleUpdateChatReadInbox(update);
        }
        return false;
    }

    public TdApi.Chat getSelectedChat() {
        return selectedChat;
    }

    public boolean setSelectedChat(TdApi.Chat selectedChat) {
        synchronized (lock) {
            if (selectedChat != null && this.selectedChat != null && selectedChat.id == this.selectedChat.id) {
                return false;
            }
            logger.debug("Updating selected chat");
            if (this.selectedChat != null && selectedChat == null) {
                telegramClientService.sendClientMessage(new TdApi.CloseChat(this.selectedChat.id));
            }
            this.selectedChat = selectedChat;
            var messagesService = ServiceLocator.getService(MessagesService.class);
            messagesService.resetLastMessage();
            if (selectedChat != null) {
                telegramClientService.sendClientMessage(new TdApi.OpenChat(selectedChat.id), null);
                telegramClientService.sendClientMessage(
                        // retrieve only the last message; this is done because in some cases tdlib sends only
                        // one message in any case at the first load, so it is better to always expect to receive
                        // only one message when the chat is opened, in order to handle message loading in a more
                        // deterministic way; next messages are requested in chunks of 20 or 10 messages
                        new TdApi.GetChatHistory(selectedChat.id, selectedChat.lastReadInboxMessageId, 0, 1, false)
                );
            }
            return true;
        }
    }

    private boolean handleUpdateChatFolders(TdApi.UpdateChatFolders update) {
        var firstLoad = chatFoldersInfo == null;
        chatFoldersInfo = update.chatFolders;
        if (firstLoad) {
            guiService.showMainWindow();
        }
        return true;
    }

    public TdApi.ChatFolderInfo[] getChatFolderInfos() {
        return chatFoldersInfo;
    }

    private boolean handleUpdateChatAddedToList(TdApi.UpdateChatAddedToList update) {
        var chat = chats.get(update.chatId);
        if (chat == null) {
            return true;
        }
        var chatFolderId = 0; // chatListMain
        if (update.chatList instanceof TdApi.ChatListFolder folder) {
            chatFolderId = folder.chatFolderId;
        } else if (!(update.chatList instanceof TdApi.ChatListMain)) {
            return true;
        }
        var list = chatFolders.get(chatFolderId);
        if (list == null) {
            list = new ArrayList<>();
            chatFolders.put(chatFolderId, list);
        }
        list.add(chat.id);
        return true;
    }

    private boolean handleUpdateChatReadInbox(TdApi.UpdateChatReadInbox update) {
        var chat = chats.get(update.chatId);
        if (chat == null) {
            return false;
        }
        chat.unreadCount = update.unreadCount;
        chat.lastReadInboxMessageId = update.lastReadInboxMessageId;
        var chatPageController = guiService.getController(ChatPageController.class);
        if (chatPageController != null) {
            chatPageController.updateChat(chat);
        }
        updateChatsListOnGui();
        return true;
    }

    public void setSelectedFolder(int selectedFolder) {
        this.selectedChatFolder = selectedFolder;
        updateChatsListOnGui();
    }

    private boolean handleUpdateNewChat(TdApi.UpdateNewChat update) {
        var chat = update.chat;
        chats.put(chat.id, chat);
        return true;
    }

    private boolean handleUpdateChatPosition(TdApi.UpdateChatPosition update) {
        if (update.position.list instanceof TdApi.ChatListFolder chatListFolder) {
            if (update.position.order == 0) {
                removeChatFromFolder(chatListFolder, update.chatId);
            }
            return true;
        }

        if (!(update.position.list instanceof TdApi.ChatListMain)) {
            return true;
        }

        TdApi.Chat chat = chats.get(update.chatId);
        if (chat == null) {
            return true;
        }

        synchronized (chat) {
            boolean found = false;
            for (var pos : chat.positions) {
                if (pos.list instanceof TdApi.ChatListMain) {
                    pos.order = update.position.order;
                    found = true;
                    break;
                }
            }
            if (!found) {
                chat.positions = Stream.concat(
                        Arrays.stream(chat.positions), Arrays.stream(new TdApi.ChatPosition[]{update.position}))
                        .toArray(TdApi.ChatPosition[]::new);
            }
        }
        updateChatsListOnGui();
        return true;
    }

    private boolean handleUpdateChatLastMessage(TdApi.UpdateChatLastMessage update) {
        TdApi.Chat chat = chats.get(update.chatId);
        if (chat == null) {
            return true;
        }
        synchronized (chat) {
            chat.positions = update.positions;
        }
        updateChatsListOnGui();
        return true;
    }

    private void removeChatFromFolder(TdApi.ChatListFolder folder, long chatId) {
        List<Long> chatIds = chatFolders.get(folder.chatFolderId);
        if (chatIds.remove(chatId)) {
            updateChatsListOnGui();
        }
    }

    private void updateChatsListOnGui() {
        Platform.runLater(() -> {
            var mainWindowController = guiService.getController(MainWindowController.class);
            if (mainWindowController != null) {
                mainWindowController.updateChatsList(getSelectedChatsList());
            }
        });
    }

    public void loadChats() {
        synchronized (loadChatsLock) {
            if (!haveFullMainChatList) {
                // send LoadChats request if there are some unknown chats and have not enough known chats
                telegramClientService.sendClientMessage(new TdApi.LoadChats(new TdApi.ChatListMain(), 20), (TdApi.Object object) -> {
                    switch (object.getConstructor()) {
                        case TdApi.Error.CONSTRUCTOR:
                            if (((TdApi.Error) object).code == 404) {
                                haveFullMainChatList = true;
                                updateChatsListOnGui();
                            } else {
                                logger.error("Receive an error for LoadChats: {}", object);
                            }
                            break;
                        case TdApi.Ok.CONSTRUCTOR:
                            // chats had already been received through updates, let's retry request
                            loadChats();
                            break;
                        default:
                            logger.error("Received unexpected response from TDLib: {}", object);
                            break;
                    }
                });
            }
        }
    }

    public TdApi.Chat getChat(long chatId) {
        return chats.get(chatId);
    }

    private Collection<TdApi.Chat> getSelectedChatsList() {
        List<Long> selectedChats = chatFolders.get(selectedChatFolder);
        if (selectedChats == null) {
            return List.of();
        }
        return chats.values().stream()
                .filter(c -> selectedChats.contains(c.id))
                .sorted((c1, c2) -> Long.compare(
                getChatPosition(c2), getChatPosition(c1)))
                .toList();
    }

    private long getChatPosition(TdApi.Chat chat) {
        for (var pos : chat.positions) {
            if (pos.list instanceof TdApi.ChatListMain) {
                return pos.order;
            }
        }
        return 0;
    }
}
