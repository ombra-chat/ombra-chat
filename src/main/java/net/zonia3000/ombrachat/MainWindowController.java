package net.zonia3000.ombrachat;

import java.io.IOError;
import java.io.IOException;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ObservableValue;
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
import net.zonia3000.ombrachat.chat.ChatFoldersBox;
import net.zonia3000.ombrachat.chat.ChatPage;
import net.zonia3000.ombrachat.chat.ChatsList;
import net.zonia3000.ombrachat.events.ChatSelected;
import net.zonia3000.ombrachat.events.WindowWidthChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainWindowController implements ErrorHandlerController {

    private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);

    private static final int MOBILE_WIDTH = 400;

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
        initSplitPaneDivider();
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

    private void initSplitPaneDivider() {
        var dividers = splitPane.getDividers();
        if (dividers.isEmpty()) {
            return;
        }
        dividers.get(0).positionProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldPosition, Number newPosition) -> {
            SplitPane.setResizableWithParent(chatsListContainer, chatsList.getWidth() < MOBILE_WIDTH);
        });
    }

    private void setWindowWidth(int windowWidth) {
        mobileMode = windowWidth < MOBILE_WIDTH;
        computeSplitPaneChildrenVisibility();
    }

    private void computeSplitPaneChildrenVisibility() {
        if (mobileMode) {
            if (mediator.getSelectedChat() == null) {
                // show only chats list
                if (!chatPageRemoved) {
                    logger.debug("Hiding chat page");
                    splitPane.getItems().remove(chatPage);
                    chatPageRemoved = true;
                }
                if (chatsListRemoved) {
                    // chat list must follow parent size when parent is resized
                    logger.debug("Displaying chats list");
                    splitPane.getItems().add(0, chatsListContainer);
                    chatsListRemoved = false;
                    initSplitPaneDivider();
                }
            } else {
                // show only chat page
                if (!chatsListRemoved) {
                    logger.debug("Hiding chats list");
                    splitPane.getItems().remove(chatsListContainer);
                    chatsListRemoved = true;
                }
                if (chatPageRemoved) {
                    logger.debug("Displaying chat page");
                    splitPane.getItems().add(chatPage);
                    chatPageRemoved = false;
                    initSplitPaneDivider();
                }
            }
        } else {
            if (chatsListRemoved) {
                logger.debug("Displaying chats list");
                splitPane.getItems().add(0, chatsListContainer);
                chatsListRemoved = false;
                initSplitPaneDivider();
            }
            if (chatPageRemoved) {
                logger.debug("Displaying chat page");
                splitPane.getItems().add(chatPage);
                chatPageRemoved = false;
                initSplitPaneDivider();
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
    private void showSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainWindowController.class.getResource("/view/settings-dialog.fxml"));
            Parent root = loader.load();
            SettingsDialogController controller = loader.getController();
            controller.setMediator(mediator);
            Scene scene = new Scene(root);
            Stage newStage = new Stage();
            newStage.setTitle("Settings");
            newStage.setScene(scene);
            newStage.show();
        } catch (IOException ex) {
            throw new IOError(ex);
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
