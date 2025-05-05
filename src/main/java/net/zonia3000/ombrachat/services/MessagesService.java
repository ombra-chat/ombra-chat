package net.zonia3000.ombrachat.services;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.controllers.ChatPageController;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagesService {

    private static final Logger logger = LoggerFactory.getLogger(MessagesService.class);

    private final Object lock = new Object();

    private long lastMessageId = 0;

    private final TelegramClientService telegramClientService;
    private final GuiService guiService;
    private final ChatsService chatsService;
    private final UsersService usersService;

    public MessagesService() {
        telegramClientService = ServiceLocator.getService(TelegramClientService.class);
        guiService = ServiceLocator.getService(GuiService.class);
        chatsService = ServiceLocator.getService(ChatsService.class);
        usersService = ServiceLocator.getService(UsersService.class);
    }

    public boolean onResult(TdApi.Object object) {
        if (chatsService.getSelectedChat() == null) {
            return false;
        }
        if (object instanceof TdApi.Messages messages) {
            return handleMessages(messages.messages);
        }
        if (object instanceof TdApi.UpdateNewMessage newMessage) {
            return handleMessages(new TdApi.Message[]{newMessage.message});
        }
        if (object instanceof TdApi.UpdateFile updateFile) {
            return handleUpdateFile(updateFile);
        }
        if (object instanceof TdApi.UpdateDeleteMessages updateDelete) {
            return handleDeleteMessages(updateDelete);
        }
        if (object instanceof TdApi.UpdateMessageSendSucceeded update) {
            return handleUpdateMessageSendSucceeded(update);
        }
        if (object instanceof TdApi.UpdateMessageInteractionInfo update) {
            return handleUpdateMessageInteractionInfo(update);
        }
        return false;
    }

    private boolean handleMessages(TdApi.Message[] messages) {
        if (messages == null) {
            return true;
        }
        synchronized (lock) {
            var selectedChat = chatsService.getSelectedChat();
            if (selectedChat == null) {
                return false;
            }
            logger.debug("Loaded {} messages", messages.length);
            if (lastMessageId == 0 && messages.length == 0) {
                // this happens when the last message has been deleted
                // let's load the chat from the last message (id == 0)
                telegramClientService.sendClientMessage(new TdApi.GetChatHistory(selectedChat.id, 0, 0, 1, false));
                return true;
            }
            List<TdApi.Message> messagesToAdd = new ArrayList<>();
            for (var message : messages) {
                if (message.chatId == selectedChat.id) {
                    if (lastMessageId == 0 && messages.length == 1) { // first request for selected chat
                        telegramClientService.sendClientMessage(new TdApi.GetChatHistory(selectedChat.id, message.id, 0, 20, false));
                    }
                    lastMessageId = message.id;
                    messagesToAdd.add(message);
                }
            }
            updateMessagesOnGui(messagesToAdd);
            return true;
        }
    }

    private void updateMessagesOnGui(List<TdApi.Message> messages) {
        Platform.runLater(() -> {
            var chatPageController = guiService.getController(ChatPageController.class);
            if (chatPageController == null) {
                return;
            }
            chatPageController.addMessages(messages);
        });
    }

    private boolean handleUpdateFile(TdApi.UpdateFile updateFile) {
        Platform.runLater(() -> {
            var chatPageController = guiService.getController(ChatPageController.class);
            if (chatPageController == null) {
                return;
            }
            chatPageController.handleFileUpdated(updateFile);
        });
        return true;
    }

    private boolean handleDeleteMessages(TdApi.UpdateDeleteMessages update) {
        Platform.runLater(() -> {
            var chatPageController = guiService.getController(ChatPageController.class);
            if (chatPageController == null) {
                return;
            }
            chatPageController.deleteMessages(update.chatId, update.messageIds);
        });
        return true;
    }

    private boolean handleUpdateMessageSendSucceeded(TdApi.UpdateMessageSendSucceeded update) {
        Platform.runLater(() -> {
            var chatPageController = guiService.getController(ChatPageController.class);
            if (chatPageController == null) {
                return;
            }
            chatPageController.updateMessage(update.oldMessageId, update.message);
        });
        return true;
    }

    private boolean handleUpdateMessageInteractionInfo(TdApi.UpdateMessageInteractionInfo update) {
        Platform.runLater(() -> {
            var chatPageController = guiService.getController(ChatPageController.class);
            if (chatPageController == null) {
                return;
            }
            if (update.interactionInfo == null || update.interactionInfo.reactions == null) {
                chatPageController.updateMessageReactions(update.messageId, null);
            } else {
                chatPageController.updateMessageReactions(update.messageId, update.interactionInfo.reactions.reactions);
            }
        });
        return true;
    }

    public void loadPreviousMessages() {
        if (lastMessageId == 0) {
            return;
        }
        logger.debug("Loading previous messages");
        telegramClientService.sendClientMessage(
                new TdApi.GetChatHistory(chatsService.getSelectedChat().id, lastMessageId, 0, 20, false)
        );
    }

    public void loadNewMessages(long messageId) {
        logger.debug("Loading new messages");
        var selectedChat = chatsService.getSelectedChat();
        telegramClientService.sendClientMessage(
                new TdApi.GetChatHistory(selectedChat.id, messageId, -10, 10, false)
        );
    }

    public String getSenderTitle(TdApi.MessageSender sender) {
        if (sender instanceof TdApi.MessageSenderChat senderChat) {
            var chat = chatsService.getChat(senderChat.chatId);
            if (chat != null) {
                return chat.title;
            }
        } else if (sender instanceof TdApi.MessageSenderUser senderUser) {
            return getSenderTitle(senderUser.userId);
        }
        logger.warn("Sender title is null {}", sender);
        return null;
    }

    public String getSenderTitle(TdApi.MessageOrigin origin) {
        if (origin instanceof TdApi.MessageOriginChannel originChannel) {
            var chat = chatsService.getChat(originChannel.chatId);
            if (chat != null) {
                return chat.title;
            }
            return originChannel.authorSignature;
        } else if (origin instanceof TdApi.MessageOriginChat originChat) {
            var chat = chatsService.getChat(originChat.senderChatId);
            if (chat != null) {
                return chat.title;
            }
        } else if (origin instanceof TdApi.MessageOriginHiddenUser originHiddenUser) {
            return originHiddenUser.senderName;
        } else if (origin instanceof TdApi.MessageOriginUser originUser) {
            return getSenderTitle(originUser.senderUserId);
        }
        logger.warn("Sender title is null {}", origin);
        return null;
    }

    public String getSenderTitle(long userId) {
        var chat = chatsService.getChat(userId);
        if (chat != null) {
            return chat.title;
        } else {
            var userLabel = usersService.getUserDisplayText(userId);
            if (userLabel != null) {
                return userLabel;
            }
        }
        return null;
    }

    public String getTextContent(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText messageText) {
            return messageText.text.text;
        }
        if (content instanceof TdApi.MessagePhoto messagePhoto) {
            if (messagePhoto.caption != null) {
                return messagePhoto.caption.text;
            }
            return null;
        }
        if (content instanceof TdApi.MessageDocument messageDocument) {
            if (messageDocument.caption != null) {
                return messageDocument.caption.text;
            }
            return null;
        }
        if (content instanceof TdApi.MessageVideo messageVideo) {
            if (messageVideo.caption != null) {
                return messageVideo.caption.text;
            }
            return null;
        }
        return null;
    }

    public void resetLastMessage() {
        lastMessageId = 0;
    }
}
