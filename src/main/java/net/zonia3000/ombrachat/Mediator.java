package net.zonia3000.ombrachat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.zonia3000.ombrachat.events.ChatFolderInfosUpdated;
import net.zonia3000.ombrachat.events.ChatFoldersBoxLoaded;
import net.zonia3000.ombrachat.events.ChatSelected;
import net.zonia3000.ombrachat.events.ChatsListComponentLoaded;
import net.zonia3000.ombrachat.events.ChatsListUpdated;
import net.zonia3000.ombrachat.events.Event;
import net.zonia3000.ombrachat.events.EventListener;
import net.zonia3000.ombrachat.events.MyIdReceived;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows for loose coupling between the components by centralizing the
 * communication logic. Acts also as event bus, managing event subscriptions and
 * notifications.
 */
public final class Mediator {

    private static final Logger logger = LoggerFactory.getLogger(Mediator.class);

    private final Map<Class<?>, List<EventListener>> eventListeners = new HashMap<>();

    private final App app;
    private final Settings settings;
    private long myId;
    private TdApi.Chat selectedChat;
    private Function<Long, TdApi.Chat> chatProvider;

    /**
     * Defines which events should be queued until a depended event happens.
     * Keys of the map represents the event to queue, values represents the
     * event to wait.
     */
    private final Map<Class<? extends Event>, Class<? extends Event>> queueRules = new ConcurrentHashMap<>();
    private final Map<Class<? extends Event>, List<Event>> queuedEvents = new ConcurrentHashMap<>();

    public Mediator(App app) {
        this.app = app;
        this.settings = new Settings();

        this.subscribe(MyIdReceived.class, (e) -> {
            this.myId = e.getId();
        });
        this.subscribe(ChatSelected.class, (e) -> {
            this.selectedChat = e.getChat();
        });

        this.queueRules.put(ChatFolderInfosUpdated.class, ChatFoldersBoxLoaded.class);
        this.queueRules.put(ChatsListUpdated.class, ChatsListComponentLoaded.class);
    }

    public synchronized <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener) {
        logger.debug("Subscribing to event {}", eventType.getSimpleName());
        eventListeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void publish(Event event) {
        var dependentEvent = queueRules.get(event.getClass());
        if (dependentEvent != null) {
            logger.debug("Queueing event {}", event.getClass().getSimpleName());
            queuedEvents.computeIfAbsent(dependentEvent, k -> new ArrayList<>()).add(event);
        } else {
            List<Event> queue = queuedEvents.remove(event.getClass());
            if (queue == null) {
                publishImmediately(event);
            } else {
                logger.debug("Found events depending on {}", event.getClass().getSimpleName());
                for (Event e : queue) {
                    queueRules.remove(e.getClass());
                    logger.debug("Dequeueing event {}", e.getClass().getSimpleName());
                    publishImmediately(e);
                }
            }
        }
    }

    private void publishImmediately(Event event) {
        List<EventListener> listeners = eventListeners.get(event.getClass());
        if (listeners != null) {
            for (EventListener listener : listeners) {
                logger.debug("Handling event {}", event.getClass().getSimpleName());
                listener.handleEvent(event);
            }
        } else {
            logger.warn("No listener defined for event {}", event.getClass().getSimpleName());
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public long getMyId() {
        return myId;
    }

    public TdApi.Chat getSelectedChat() {
        return selectedChat;
    }

    public void showDocument(String url) {
        app.getHostServices().showDocument(url);
    }

    public String getChatKeyFingerprint() {
        return settings.getChatKey(selectedChat.id);
    }

    public void registerChatProvider(Function<Long, TdApi.Chat> chatProvider) {
        this.chatProvider = chatProvider;
    }

    public TdApi.Chat getChat(long id) {
        return chatProvider.apply(id);
    }
}
