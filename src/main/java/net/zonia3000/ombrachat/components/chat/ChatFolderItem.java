package net.zonia3000.ombrachat.components.chat;

public class ChatFolderItem {

    private final int id;
    private final String label;

    public ChatFolderItem(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
