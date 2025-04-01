package net.zonia3000.ombrachat.events;

import java.util.Collection;
import net.zonia3000.ombrachat.events.Event;
import org.drinkless.tdlib.TdApi;

public class ChatsListUpdated implements Event {

    private final Collection<TdApi.Chat> chats;

    public ChatsListUpdated(Collection<TdApi.Chat> chats) {
        this.chats = chats;
    }

    public Collection<TdApi.Chat> getChats() {
        return chats;
    }
}
