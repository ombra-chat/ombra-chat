package net.zonia3000.ombrachat.events;

public class MessagesDeleted implements Event {

    private final long chatId;
    private final long[] messageIds;

    public MessagesDeleted(long chatId, long[] messageIds) {
        this.chatId = chatId;
        this.messageIds = messageIds;
    }

    public long getChatId() {
        return chatId;
    }

    public long[] getMessageIds() {
        return messageIds;
    }
}
