package net.zonia3000.ombrachat;

import java.util.function.Consumer;
import javafx.application.Platform;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

public class MessagesLoader {

    private final Client client;

    private final Object lock = new Object();

    private long lastMessageId = 0;
    private TdApi.Chat selectedChat;

    private Consumer<TdApi.Message> messageConsumer;

    public MessagesLoader(Client client) {
        this.client = client;
    }

    public void setMessageConsumer(Consumer<TdApi.Message> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    public void setSelectedChat(TdApi.Chat selectedChat) {
        synchronized (lock) {
            this.selectedChat = selectedChat;
            lastMessageId = 0;
            client.send(new TdApi.OpenChat(selectedChat.id), null);
            client.send(new TdApi.GetChatHistory(selectedChat.id, 0, 0, 20, false),
                    (TdApi.Object object) -> {
                        MessagesLoader.this.onResult(object);
                    });
        }
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

    private boolean handleMessages(TdApi.Messages messages) {
        synchronized (lock) {
            if (selectedChat == null) {
                return false;
            }
            for (var message : messages.messages) {
                if (message.chatId == selectedChat.id) {
                    if (lastMessageId == 0) { // first request for selected chat
                        client.send(new TdApi.GetChatHistory(selectedChat.id, message.id, 0, 20, false),
                                (TdApi.Object object) -> {
                                    MessagesLoader.this.onResult(object);
                                });
                    }
                    lastMessageId = message.id;
                    if (messageConsumer != null) {
                        Platform.runLater(() -> {
                            messageConsumer.accept(message);
                        });
                    }
                }
            }
            return true;
        }
    }
}
