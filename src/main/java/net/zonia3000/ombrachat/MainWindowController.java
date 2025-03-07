package net.zonia3000.ombrachat;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.util.Duration;
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
    @FXML
    private Label usernameLabel;
    @FXML
    private AnchorPane sidebar;
    
    private boolean messagesContainerRemoved;
    
    private Settings settings;
    
    @FXML
    public void initialize() {
        initSidebar();
    }
    
    public void setSettings(Settings settings) {
        this.settings = settings;
    }
    
    public void setLoaders(ChatsLoader chatsLoader, MessagesLoader messagesLoader) {
        chatFolders.setChatsLoader(chatsLoader);
        chatsList.setLoaders(chatsLoader, messagesLoader);
        messagesLoader.setChatPage(chatPage);
        chatPage.setMessagesLoader(messagesLoader);
        chatPage.setChatsLoader(chatsLoader);
        chatPage.setClient(chatsLoader.getClient());
        chatPage.setSettings(settings);
        VBox.setVgrow(chatsList, Priority.ALWAYS);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
    }
    
    public void setMyId(long myId) {
        chatPage.setMyId(myId);
    }
    
    public void setMyUsername(String myUsername) {
        usernameLabel.setText(myUsername);
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
    
    private void initSidebar() {
        // setting sidebar width to avoid overflow on small screens
        Rectangle2D screenBounds = Screen.getPrimary().getBounds();
        double screenWidth = screenBounds.getWidth();
        double sidebarWidth = Math.min(screenWidth, 250);
        sidebar.setPrefWidth(sidebarWidth);
        sidebar.setTranslateX(-1 * screenWidth);
    }
    
    @FXML
    private void toggleSidebar() {
        TranslateTransition openNav = new TranslateTransition(new Duration(350), sidebar);
        openNav.setToX(0);
        TranslateTransition closeNav = new TranslateTransition(new Duration(350), sidebar);
        if (sidebar.getTranslateX() != 0) {
            openNav.play();
        } else {
            closeNav.setToX(-(sidebar.getWidth()));
            closeNav.play();
        }
    }
    
    @Override
    public void displayError(String error) {
        // TODO
    }
}
