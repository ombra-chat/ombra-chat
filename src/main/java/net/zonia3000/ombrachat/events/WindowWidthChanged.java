package net.zonia3000.ombrachat.events;

public class WindowWidthChanged implements Event {

    private final int width;

    public WindowWidthChanged(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }
}
