package net.zonia3000.ombrachat.events;

import org.drinkless.tdlib.TdApi;

public class ChatFolderInfosUpdated implements Event {

    private final TdApi.ChatFolderInfo[] chatFolderInfos;

    public ChatFolderInfosUpdated(TdApi.ChatFolderInfo[] chatFolderInfos) {
        this.chatFolderInfos = chatFolderInfos;
    }

    public TdApi.ChatFolderInfo[] getChatFolderInfos() {
        return chatFolderInfos;
    }
}
