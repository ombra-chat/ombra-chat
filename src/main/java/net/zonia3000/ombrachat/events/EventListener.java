package net.zonia3000.ombrachat.events;

public interface EventListener<E extends Event> {

    void handleEvent(E event);
}
