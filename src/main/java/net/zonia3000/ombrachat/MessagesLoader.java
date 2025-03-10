package net.zonia3000.ombrachat;

import net.zonia3000.ombrachat.events.ChatSelected;
import net.zonia3000.ombrachat.events.LoadPreviousMessages;
import net.zonia3000.ombrachat.events.MessageReceived;
import net.zonia3000.ombrachat.events.SendClientMessage;
import org.drinkless.tdlib.TdApi;

public class MessagesLoader {

    private final Object lock = new Object();

    private long lastMessageId = 0;
    private TdApi.Chat selectedChat;

    private final Mediator mediator;

    public MessagesLoader(Mediator mediator) {
        this.mediator = mediator;
        mediator.subscribe(LoadPreviousMessages.class, (e) -> loadPreviousMessages());
        mediator.subscribe(ChatSelected.class, (e) -> setSelectedChat(e.getChat()));
    }

    public boolean onResult(TdApi.Object object) {
        if (selectedChat == null) {
            return false;
        }
        if (object instanceof TdApi.Messages messages) {
            return handleMessages(messages);
        }
        return false;
    }

    private void setSelectedChat(TdApi.Chat selectedChat) {
        synchronized (lock) {
            this.selectedChat = selectedChat;
            lastMessageId = 0;
            mediator.publish(new SendClientMessage(new TdApi.OpenChat(selectedChat.id), null));
            mediator.publish(new SendClientMessage(new TdApi.GetChatHistory(selectedChat.id, 0, 0, 20, false),
                    (TdApi.Object object) -> {
                        MessagesLoader.this.onResult(object);
                    }));
        }
    }

    private boolean handleMessages(TdApi.Messages messages) {
        synchronized (lock) {
            if (selectedChat == null) {
                return false;
            }
            for (var message : messages.messages) {
                if (message.chatId == selectedChat.id) {
                    if (lastMessageId == 0) { // first request for selected chat
                        mediator.publish(new SendClientMessage(new TdApi.GetChatHistory(selectedChat.id, message.id, 0, 20, false),
                                (TdApi.Object object) -> {
                                    MessagesLoader.this.onResult(object);
                                }));
                    }
                    lastMessageId = message.id;
                    mediator.publish(new MessageReceived(message));
                }
            }
            return true;
        }
    }

    private void loadPreviousMessages() {
        if (lastMessageId == 0) {
            return;
        }
        mediator.publish(new SendClientMessage(new TdApi.GetChatHistory(selectedChat.id, lastMessageId, 0, 20, false),
                (TdApi.Object object) -> {
                    MessagesLoader.this.onResult(object);
                }));
    }
}
