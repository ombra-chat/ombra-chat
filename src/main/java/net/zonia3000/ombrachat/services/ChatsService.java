package net.zonia3000.ombrachat.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.events.ChatSelected;
import net.zonia3000.ombrachat.events.SelectedChatFolderChanged;
import net.zonia3000.ombrachat.events.telegram.ChatsListUpdated;
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
        guiService.subscribe(SelectedChatFolderChanged.class, (e) -> setSelectedFolder(e.getId()));
    }

    public boolean onResult(TdApi.Object object) {
        if (object instanceof TdApi.UpdateChatFolders update) {
            handleUpdateChatFolders(update);
        } else if (object instanceof TdApi.UpdateNewChat update) {
            return handleUpdateNewChat(update);
        } else if (object instanceof TdApi.UpdateChatPosition update) {
            return handleUpdateChatPosition(update);
        } else if (object instanceof TdApi.UpdateChatAddedToList update) {
            return handleUpdateChatAddedToList(update);
        }
        return false;
    }

    public TdApi.Chat getSelectedChat() {
        return selectedChat;
    }

    public void setSelectedChat(TdApi.Chat selectedChat) {
        synchronized (lock) {
            this.selectedChat = selectedChat;
            guiService.publish(new ChatSelected(selectedChat));
            var messagesService = ServiceLocator.getService(MessagesService.class);
            messagesService.resetLastMessage();
            if (selectedChat != null) {
                telegramClientService.sendClientMessage(new TdApi.OpenChat(selectedChat.id), null);
                telegramClientService.sendClientMessage(new TdApi.GetChatHistory(selectedChat.id, 0, 0, 20, false),
                        (TdApi.Object object) -> {
                            messagesService.onResult(object);
                        });
            }
        }
    }

    private boolean handleUpdateChatFolders(TdApi.UpdateChatFolders update) {
        chatFoldersInfo = update.chatFolders;
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

    public void setSelectedFolder(int selectedFolder) {
        this.selectedChatFolder = selectedFolder;
        this.guiService.publish(new ChatsListUpdated(getSelectedChatsList()));
    }

    private boolean handleUpdateNewChat(TdApi.UpdateNewChat update) {
        var chat = update.chat;
        chats.put(chat.id, chat);
        return true;
    }

    private boolean handleUpdateChatPosition(TdApi.UpdateChatPosition update) {
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
                if (update.position.list instanceof TdApi.ChatListMain && pos.list instanceof TdApi.ChatListMain) {
                    pos.order = update.position.order;
                    found = true;
                }
            }
            if (!found) {
                chat.positions = Stream.concat(
                        Arrays.stream(chat.positions), Arrays.stream(new TdApi.ChatPosition[]{update.position}))
                        .toArray(TdApi.ChatPosition[]::new);
            }
        }
        return true;
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
                                guiService.publish(new ChatsListUpdated(getSelectedChatsList()));
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
