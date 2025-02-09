package net.zonia3000.ombrachat.components.chat;

import java.io.IOError;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import net.zonia3000.ombrachat.ChatsLoader;
import org.drinkless.tdlib.TdApi;

public class ChatFoldersBox extends HBox {

    private static class ChatFolderItem {

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

    @FXML
    private ComboBox chatFolderComboBox;

    private ChatsLoader chatsLoader;

    private TdApi.ChatFolderInfo[] chatFolderInfos;

    public ChatFoldersBox() {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatFoldersBox.class.getResource("/view/chat-folders.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }

    public void setChatsLoader(ChatsLoader chatsLoader) {
        this.chatsLoader = chatsLoader;
        this.chatsLoader.setChatFoldersConsumer(this::setChatFolders);
    }

    public void setChatFolders(TdApi.ChatFolderInfo[] chatFolderInfos) {
        this.chatFolderInfos = chatFolderInfos;

        chatFolderComboBox.getItems().add(new ChatFolderItem(0, "All"));
        chatFolderComboBox.setValue("All");
        for (var chatFolderInfo : chatFolderInfos) {
            chatFolderComboBox.getItems().add(new ChatFolderItem(chatFolderInfo.id, chatFolderInfo.name.text.text));
        }

        chatFolderComboBox.setOnAction(event -> {
            ChatFolderItem selectedItem = (ChatFolderItem) chatFolderComboBox.getValue();
            chatsLoader.onSelectedFolderChanged(selectedItem.id);
        });
    }
}
