package net.zonia3000.ombrachat.events;

public class ErrorReceived implements Event {

    private final String error;

    public ErrorReceived(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
