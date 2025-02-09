package net.zonia3000.ombrachat;

import javafx.fxml.FXML;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.zonia3000.ombrachat.components.chat.ChatFoldersBox;
import net.zonia3000.ombrachat.components.chat.ChatsList;

public class MainWindowController implements ErrorHandlerController {

    @FXML
    private ChatFoldersBox chatFolders;
    @FXML
    private ChatsList chatsList;

    private ChatsLoader chatsLoader;

    public void setChatsLoader(ChatsLoader chatsLoader) {
        this.chatsLoader = chatsLoader;
        chatFolders.setChatsLoader(chatsLoader);
        chatsList.setChatsLoader(chatsLoader);
        VBox.setVgrow(chatsList, Priority.ALWAYS);
    }

    @Override
    public void displayError(String error) {
        // TODO
    }
}
