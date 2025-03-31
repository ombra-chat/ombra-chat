package net.zonia3000.ombrachat.services;

import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.events.MessageReceived;
import org.drinkless.tdlib.TdApi;

public class MessagesService {

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
            return handleMessages(messages);
        }
        return false;
    }

    private boolean handleMessages(TdApi.Messages messages) {
        synchronized (lock) {
            var selectedChat = chatsService.getSelectedChat();
            if (selectedChat == null) {
                return false;
            }
            for (var message : messages.messages) {
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
