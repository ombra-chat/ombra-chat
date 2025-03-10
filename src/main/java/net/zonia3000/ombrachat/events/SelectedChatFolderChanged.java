package net.zonia3000.ombrachat.events;

public class SelectedChatFolderChanged implements Event {

    private final int id;

    public SelectedChatFolderChanged(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
