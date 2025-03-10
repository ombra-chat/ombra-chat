package net.zonia3000.ombrachat;

import java.io.IOError;
import java.io.IOException;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.zonia3000.ombrachat.components.chat.ChatFoldersBox;
import net.zonia3000.ombrachat.components.chat.ChatPage;
import net.zonia3000.ombrachat.components.chat.ChatsList;
import net.zonia3000.ombrachat.events.ChatSelected;
import net.zonia3000.ombrachat.events.WindowWidthChanged;

public class MainWindowController implements ErrorHandlerController {

    private Mediator mediator;

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

    // used to implement responsive behavior
    private boolean mobileMode;
    private boolean chatPageRemoved;
    private boolean chatsListRemoved;

    @FXML
    public void initialize() {
        initSidebar();
        VBox.setVgrow(chatsList, Priority.ALWAYS);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
    }

    public void setMediator(Mediator mediator) {
        this.mediator = mediator;
        mediator.subscribe(ChatSelected.class, (e) -> computeSplitPaneChildrenVisibility());
        mediator.subscribe(WindowWidthChanged.class, (e) -> setWindowWidth(e.getWidth()));
        chatFolders.setMediator(mediator);
        chatsList.setMediator(mediator);
        chatPage.setMediator(mediator);
    }

    private void setWindowWidth(int windowWidth) {
        mobileMode = windowWidth < 400;
        computeSplitPaneChildrenVisibility();
    }

    private void computeSplitPaneChildrenVisibility() {
        if (mobileMode) {
            if (mediator.getSelectedChat() == null) {
                // show only chats list
                if (!chatPageRemoved) {
                    splitPane.getItems().remove(chatPage);
                    chatPageRemoved = true;
                }
                if (chatsListRemoved) {
                    // chat list must follow parent size when parent is resized
                    SplitPane.setResizableWithParent(chatsListContainer, Boolean.TRUE);
                    splitPane.getItems().add(chatsListContainer);
                    chatsListRemoved = false;
                }
            } else {
                // show only chat page
                if (!chatsListRemoved) {
                    splitPane.getItems().remove(chatsListContainer);
                    chatsListRemoved = true;
                }
                if (chatPageRemoved) {
                    splitPane.getItems().add(chatPage);
                    chatPageRemoved = false;
                }
            }
        } else {
            if (chatsListRemoved) {
                // chat list should not be resized when parent is resized
                SplitPane.setResizableWithParent(chatsListContainer, Boolean.FALSE);
                splitPane.getItems().add(chatsListContainer);
                chatsListRemoved = false;
            }
            if (chatPageRemoved) {
                splitPane.getItems().add(chatPage);
                chatPageRemoved = false;
            }
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

    @FXML
    private void showAboutDialog() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainWindowController.class.getResource("/view/about.fxml"));
            Parent root = loader.load();
            Label versionLabel = (Label) root.lookup("#versionLabel");
            versionLabel.setText("Version: " + UiUtils.getVersion());
            Hyperlink link = (Hyperlink) root.lookup("#icons8link");
            link.setOnAction(event -> mediator.showDocument("https://icons8.com"));
            Scene aboutScene = new Scene(root);
            UiUtils.setCommonCss(aboutScene);
            Stage aboutStage = new Stage();
            aboutStage.setTitle("About");
            aboutStage.setScene(aboutScene);
            aboutStage.initOwner(aboutStage.getOwner());
            aboutStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            aboutStage.showAndWait();
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }

    @Override
    public void displayError(String error) {
        // TODO
    }
}
