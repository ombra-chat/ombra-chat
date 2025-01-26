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
        this.chatsLoader.onChatFoldersBoxReady((f) -> setChatFolders(f));
    }

    public void setChatFolders(TdApi.ChatFolderInfo[] chatFolderInfos) {
        this.chatFolderInfos = chatFolderInfos;
        chatFolderComboBox.getItems().add("All");
        for (var chatFolderInfo : chatFolderInfos) {
            chatFolderComboBox.getItems().add(chatFolderInfo.name.text.text);
        }
    }
}
