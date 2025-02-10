package net.zonia3000.ombrachat;

import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.zonia3000.ombrachat.components.chat.ChatFoldersBox;
import net.zonia3000.ombrachat.components.chat.ChatPage;
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
    private ChatPage chatPage;
    boolean messagesContainerRemoved;

    private ChatsLoader chatsLoader;
    private MessagesLoader messagesLoader;

    public void setLoaders(ChatsLoader chatsLoader, MessagesLoader messagesLoader) {
        this.chatsLoader = chatsLoader;
        this.messagesLoader = messagesLoader;
        chatFolders.setChatsLoader(chatsLoader);
        chatsList.setLoaders(chatsLoader, messagesLoader);
        chatPage.setMessagesLoader(messagesLoader);
        VBox.setVgrow(chatsList, Priority.ALWAYS);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
    }

    public void setMyId(long myId) {
        chatPage.setMyId(myId);
    }

    public void setWindowWidth(int windowWidth) {
        if (windowWidth > 400) {
            if (messagesContainerRemoved) {
                SplitPane.setResizableWithParent(chatsListContainer, Boolean.FALSE);
                splitPane.getItems().add(chatPage);
                messagesContainerRemoved = false;
            }
        } else if (!messagesContainerRemoved) {
            SplitPane.setResizableWithParent(chatsListContainer, Boolean.TRUE);
            splitPane.getItems().remove(chatPage);
            messagesContainerRemoved = true;
        }
    }

    @Override
    public void displayError(String error) {
        // TODO
    }
}
