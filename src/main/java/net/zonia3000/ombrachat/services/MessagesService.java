package net.zonia3000.ombrachat.services;

import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.events.FileUpdated;
import net.zonia3000.ombrachat.events.MessageReceived;
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

    public MessagesService() {
        telegramClientService = ServiceLocator.getService(TelegramClientService.class);
        guiService = ServiceLocator.getService(GuiService.class);
        chatsService = ServiceLocator.getService(ChatsService.class);
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
        return false;
    }

    private boolean handleMessages(TdApi.Message[] messages) {
        synchronized (lock) {
            var selectedChat = chatsService.getSelectedChat();
            if (selectedChat == null) {
                return false;
            }
            for (var message : messages) {
                if (message.chatId == selectedChat.id) {
                    if (lastMessageId == 0) { // first request for selected chat
                        telegramClientService.sendClientMessage(new TdApi.GetChatHistory(selectedChat.id, message.id, 0, 20, false),
                                (TdApi.Object object) -> {
                                    MessagesService.this.onResult(object);
                                });
                    }
                    lastMessageId = message.id;
                    guiService.publish(new MessageReceived(message));
                }
            }
            return true;
        }
    }

    private boolean handleUpdateFile(TdApi.UpdateFile updateFile) {
        guiService.publish(new FileUpdated(updateFile));
        return true;
    }

    public void loadPreviousMessages() {
        if (lastMessageId == 0) {
            return;
        }
        telegramClientService.sendClientMessage(new TdApi.GetChatHistory(chatsService.getSelectedChat().id, lastMessageId, 0, 20, false),
                (TdApi.Object object) -> {
                    MessagesService.this.onResult(object);
                });
    }

    public void resetLastMessage() {
        lastMessageId = 0;
    }
}
