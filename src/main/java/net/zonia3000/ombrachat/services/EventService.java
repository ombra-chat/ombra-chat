package net.zonia3000.ombrachat.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.application.Platform;
import net.zonia3000.ombrachat.events.Event;
import net.zonia3000.ombrachat.events.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final Map<Class<?>, List<EventListener>> telegramEventListeners = new HashMap<>();
    private final Map<Class<?>, List<EventListener>> guiEventListeners = new HashMap<>();

    public void sendEventToTelegram(Event event) {
        List<EventListener> listeners = guiEventListeners.get(event.getClass());
        if (listeners != null) {
            for (EventListener listener : listeners) {
                logger.debug("Handling event {}", event.getClass().getSimpleName());
                listener.handleEvent(event);
            }
        } else {
            logger.warn("No listener defined for event {}", event.getClass().getSimpleName());
        }
    }

    public void sendEventToGui(Event event) {
        List<EventListener> listeners = telegramEventListeners.get(event.getClass());
        if (listeners != null) {
            for (EventListener listener : listeners) {
                Platform.runLater(() -> {
                    logger.debug("Handling event {}", event.getClass().getSimpleName());
                    listener.handleEvent(event);
                });
            }
        } else {
            logger.warn("No listener defined for event {}", event.getClass().getSimpleName());
        }
    }

    public <T extends Event> void reactToGuiEvent(Class<T> guiEventType, Function<T, Event> telegramEventProvider) {
        subscribeToGuiEvent(guiEventType, (guiEvent) -> {
            sendEventToGui(telegramEventProvider.apply(guiEvent));
        });
    }

    public synchronized <T extends Event> void subscribeToTelegramEvent(Class<T> eventType, EventListener<T> listener) {
        logger.debug("Subscribing to Telegram event {}", eventType.getSimpleName());
        telegramEventListeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public synchronized <T extends Event> void subscribeToGuiEvent(Class<T> eventType, EventListener<T> listener) {
        logger.debug("Subscribing to GUI event {}", eventType.getSimpleName());
        guiEventListeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }
}
