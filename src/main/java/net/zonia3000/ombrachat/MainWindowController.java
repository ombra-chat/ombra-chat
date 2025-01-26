package net.zonia3000.ombrachat;

import javafx.fxml.FXML;
import net.zonia3000.ombrachat.components.chat.ChatFoldersBox;

public class MainWindowController implements ErrorHandlerController {

    @FXML
    private ChatFoldersBox chatFolders;

    private ChatsLoader chatsLoader;

    public void setChatsLoader(ChatsLoader chatsLoader) {
        this.chatsLoader = chatsLoader;
        chatFolders.setChatsLoader(chatsLoader);
    }

    @Override
    public void displayError(String error) {
        // TODO
    }
}
