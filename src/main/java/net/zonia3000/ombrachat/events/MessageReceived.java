package net.zonia3000.ombrachat.events;

import org.drinkless.tdlib.TdApi.Message;

public class MessageReceived implements Event {

    private final Message message;

    public MessageReceived(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
