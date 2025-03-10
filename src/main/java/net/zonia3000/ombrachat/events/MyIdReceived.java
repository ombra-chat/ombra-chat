package net.zonia3000.ombrachat.events;

public class MyIdReceived implements Event {

    private final long id;

    public MyIdReceived(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
