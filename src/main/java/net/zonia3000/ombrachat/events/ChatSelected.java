package net.zonia3000.ombrachat.events;

import org.drinkless.tdlib.TdApi.Chat;

public class ChatSelected implements Event {

    private final Chat chat;

    public ChatSelected(Chat chat) {
        this.chat = chat;
    }

    public Chat getChat() {
        return chat;
    }
}
