package net.zonia3000.ombrachat;

import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.zonia3000.ombrachat.components.chat.ChatFoldersBox;
import net.zonia3000.ombrachat.components.chat.ChatsList;

public class MainWindowController implements ErrorHandlerController {

    @FXML
    private ChatFoldersBox chatFolders;
    @FXML
    private VBox chatsListContainer;
    @FXML
    private ChatsList chatsList;
    @FXML
    private SplitPane splitPane;
    @FXML
    private VBox messagesContainer;
    boolean messagesContainerRemoved;

    private ChatsLoader chatsLoader;

    public void setChatsLoader(ChatsLoader chatsLoader) {
        this.chatsLoader = chatsLoader;
        chatFolders.setChatsLoader(chatsLoader);
        chatsList.setChatsLoader(chatsLoader);
        VBox.setVgrow(chatsList, Priority.ALWAYS);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
    }

    public void setWindowWidth(int windowWidth) {
        if (windowWidth > 400) {
            if (messagesContainerRemoved) {
                SplitPane.setResizableWithParent(chatsListContainer, Boolean.FALSE);
                splitPane.getItems().add(messagesContainer);
                messagesContainerRemoved = false;
            }
        } else if (!messagesContainerRemoved) {
            SplitPane.setResizableWithParent(chatsListContainer, Boolean.TRUE);
            splitPane.getItems().remove(messagesContainer);
            messagesContainerRemoved = true;
        }
    }

    @Override
    public void displayError(String error) {
        // TODO
    }
}
