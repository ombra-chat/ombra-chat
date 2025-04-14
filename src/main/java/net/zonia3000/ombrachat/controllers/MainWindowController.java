package net.zonia3000.ombrachat.controllers;

import java.io.IOError;
import java.io.IOException;
import java.util.Collection;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.chat.ChatsListView;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.GuiService;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainWindowController implements ErrorHandlerController {

    private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);

    private static final int MOBILE_WIDTH = 400;

    @FXML
    private HBox chatFolders;
    @FXML
    private VBox chatsListContainer;
    @FXML
    private ChatsListView chatsList;
    @FXML
    private SplitPane splitPane;
    @FXML
    private VBox chatPage;
    @FXML
    private Label usernameLabel;
    @FXML
    private AnchorPane sidebar;

    // used to implement responsive behavior
    private boolean mobileMode;
    private boolean chatPageRemoved;
    private boolean chatsListRemoved;

    private final GuiService guiService;
    private final ChatsService chatsService;

    public MainWindowController() {
        guiService = ServiceLocator.getService(GuiService.class);
        chatsService = ServiceLocator.getService(ChatsService.class);
    }

    @FXML
    public void initialize() {
        initSidebar();
        initSplitPaneDivider();
        initChatFoldersBox();
        initChatPage();
        VBox.setVgrow(chatsList, Priority.ALWAYS);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        guiService.registerController(MainWindowController.class, this);
        logger.debug("{} initialized", MainWindowController.class.getSimpleName());
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

    private void initChatFoldersBox() {
        ChatFoldersBoxController chatFoldersBoxController = new ChatFoldersBoxController();
        ServiceLocator.registerService(ChatFoldersBoxController.class, chatFoldersBoxController);
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainWindowController.class.getResource("/view/chat-folders.fxml"));
            fxmlLoader.setController(chatFoldersBoxController);
            fxmlLoader.setRoot(chatFolders);
            fxmlLoader.load();
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }

    private void initChatPage() {
        ChatPageController chatPageController = new ChatPageController(chatPage);
        ServiceLocator.registerService(ChatPageController.class, chatPageController);
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainWindowController.class.getResource("/view/chat-page.fxml"));
            fxmlLoader.setController(chatPageController);
            fxmlLoader.setRoot(chatPage);
            fxmlLoader.load();
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }

    public void updateChatsList(Collection<TdApi.Chat> collection) {
        if (chatsList == null) {
            return;
        }
        chatsList.setChatsList(collection);
    }

    public void onWindowWidthChanged(int windowWidth) {
        mobileMode = windowWidth < MOBILE_WIDTH;
        computeSplitPaneChildrenVisibility();
    }

    public void onChatSelected() {
        computeSplitPaneChildrenVisibility();
    }

    private void computeSplitPaneChildrenVisibility() {
        if (mobileMode) {
            if (chatsService.getSelectedChat() == null) {
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
            logger.debug("opening sidebar");
            openNav.play();
        } else {
            logger.debug("closing sidebar");
            closeNav.setToX(-(sidebar.getWidth()));
            closeNav.play();
        }
    }

    @FXML
    private void showSettingsDialog() {
        try {
            logger.debug("Showing settings dialog");
            FXMLLoader loader = new FXMLLoader(MainWindowController.class.getResource("/view/settings-dialog.fxml"));
            Parent root = loader.load();
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
            logger.debug("Showing about dialog");
            FXMLLoader loader = new FXMLLoader(MainWindowController.class.getResource("/view/about.fxml"));
            Parent root = loader.load();
            Label versionLabel = (Label) root.lookup("#versionLabel");
            versionLabel.setText("Version: " + UiUtils.getVersion());
            Hyperlink link = (Hyperlink) root.lookup("#icons8link");
            link.setOnAction(event -> guiService.showDocument("https://icons8.com"));
            ImageView logoView = (ImageView) root.lookup("#logoImageView");
            logoView.setImage(UiUtils.getAppIcon());
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

    @FXML
    private void showLogoutDialog() {
        try {
            logger.debug("Showing logout dialog");
            FXMLLoader loader = new FXMLLoader(MainWindowController.class.getResource("/view/logout.fxml"));
            Parent root = loader.load();
            Scene logoutScene = new Scene(root);
            UiUtils.setCommonCss(logoutScene);
            Stage logoutStage = new Stage();
            logoutStage.setTitle("About");
            logoutStage.setScene(logoutScene);
            logoutStage.initOwner(logoutStage.getOwner());
            logoutStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            logoutStage.showAndWait();
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }

    @Override
    public void displayError(String error) {
        // TODO
    }
}
