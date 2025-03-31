package net.zonia3000.ombrachat.events.gui;

import net.zonia3000.ombrachat.events.Event;

public class WindowWidthChanged implements Event {

    private final int width;

    public WindowWidthChanged(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }
}
