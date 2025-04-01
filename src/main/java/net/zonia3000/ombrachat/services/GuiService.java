package net.zonia3000.ombrachat.services;

import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.App;
import net.zonia3000.ombrachat.controllers.ErrorHandlerController;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.events.Event;
import net.zonia3000.ombrachat.events.EventListener;
import net.zonia3000.ombrachat.events.WindowWidthChanged;
import net.zonia3000.ombrachat.controllers.AuthenticationCodeController;
import net.zonia3000.ombrachat.controllers.AuthenticationPasswordController;
import net.zonia3000.ombrachat.controllers.PhoneDialogController;
import net.zonia3000.ombrachat.controllers.MainWindowController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuiService {

    private static final Logger logger = LoggerFactory.getLogger(GuiService.class);

    private final Map<Class<?>, List<EventListener>> telegramEventListeners = new HashMap<>();

    private final App app;
    private final Stage primaryStage;

    private ErrorHandlerController currentController;

    public GuiService(App app, Stage primaryStage) {
        this.app = app;
        this.primaryStage = primaryStage;
    }

    public void handleError(String errorMessage) {
        Platform.runLater(() -> {
            logger.warn("Received error: {}", errorMessage);
            if (currentController == null) {
                return;
            }
            currentController.displayError(errorMessage);
        });
    }

    public void showPhoneNumberDialog() {
        Platform.runLater(() -> {
            try {
                logger.debug("Showing phone number dialog");
                FXMLLoader loader = new FXMLLoader(GuiService.class.getResource("/view/login/phone-dialog.fxml"));
                Parent root = loader.load();
                PhoneDialogController controller = loader.getController();
                currentController = controller;

                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.show();
            } catch (IOException ex) {
                throw new IOError(ex);
            }
        });
    }

    public void showAuthenticationCodeDialog() {
        Platform.runLater(() -> {
            try {
                logger.debug("Showing authentication code dialog");
                FXMLLoader loader = new FXMLLoader(GuiService.class.getResource("/view/login/authentication-code-dialog.fxml"));
                Parent root = loader.load();
                AuthenticationCodeController controller = loader.getController();
                currentController = controller;

                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.show();
            } catch (IOException ex) {
                throw new IOError(ex);
            }
        });
    }

    public void showAuthenticationPasswordDialog() {
        Platform.runLater(() -> {
            try {
                logger.debug("Showing authentication password dialog");
                FXMLLoader loader = new FXMLLoader(GuiService.class.getResource("/view/login/authentication-password-dialog.fxml"));
                Parent root = loader.load();
                AuthenticationPasswordController controller = loader.getController();
                currentController = controller;

                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.show();
            } catch (IOException ex) {
                throw new IOError(ex);
            }
        });
    }

    public void showMainWindow() {
        Platform.runLater(() -> {
            try {
                logger.debug("Showing main window");
                FXMLLoader loader = new FXMLLoader(GuiService.class.getResource("/view/main-window.fxml"));
                Parent root = loader.load();
                MainWindowController controller = loader.getController();
                currentController = controller;

                Scene scene = new Scene(root);
                UiUtils.setCommonCss(scene);

                scene.widthProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) -> {
                    publish(new WindowWidthChanged(newSceneWidth.intValue()));
                });

                // Detect escape key pressed
                scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        // hide chat
                        var chatService = ServiceLocator.getService(ChatsService.class);
                        chatService.setSelectedChat(null);
                    }
                });

                primaryStage.setScene(scene);
                primaryStage.setTitle("OmbraChat");
                primaryStage.show();

            } catch (IOException ex) {
                throw new IOError(ex);
            }
        });
    }

    public void publish(Event event) {
        List<EventListener> listeners = telegramEventListeners.get(event.getClass());
        if (listeners != null) {
            for (EventListener listener : listeners) {
                Platform.runLater(() -> {
                    logger.debug("Handling event {}", event.getClass().getSimpleName());
                    listener.handleEvent(event);
                });
            }
        } else {
            logger.warn("No listener defined for event {}", event.getClass().getSimpleName());
        }
    }

    public synchronized <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener) {
        logger.debug("Subscribing to Telegram event {}", eventType.getSimpleName());
        telegramEventListeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void showDocument(String url) {
        app.getHostServices().showDocument(url);
    }
}
