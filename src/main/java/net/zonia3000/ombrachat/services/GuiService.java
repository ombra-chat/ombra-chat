package net.zonia3000.ombrachat.services;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.App;
import net.zonia3000.ombrachat.controllers.ErrorHandlerController;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.chat.ChatsListView;
import net.zonia3000.ombrachat.components.SelectableText;
import net.zonia3000.ombrachat.controllers.AuthenticationCodeController;
import net.zonia3000.ombrachat.controllers.AuthenticationPasswordController;
import net.zonia3000.ombrachat.controllers.EncryptionPasswordController;
import net.zonia3000.ombrachat.controllers.InitialConfigDialogController;
import net.zonia3000.ombrachat.controllers.MainWindowController;
import net.zonia3000.ombrachat.controllers.PhoneDialogController;
import net.zonia3000.ombrachat.events.Event;
import net.zonia3000.ombrachat.events.EventListener;
import net.zonia3000.ombrachat.events.WindowWidthChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuiService {

    private static final Logger logger = LoggerFactory.getLogger(GuiService.class);

    private final Map<Class<?>, List<EventListener>> eventListeners = new HashMap<>();

    private final App app;
    private final Stage primaryStage;

    private ErrorHandlerController currentController;

    private final Image lockImage;

    private SelectableText currentSelectable;

    public GuiService(App app, Stage primaryStage) {
        this.app = app;
        this.primaryStage = primaryStage;
        lockImage = createLockImage();
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

    public void showInitialConfigDialog() {
        Platform.runLater(() -> {
            try {
                if (currentController instanceof InitialConfigDialogController) {
                    return;
                }
                logger.debug("Showing initial config dialog");
                FXMLLoader loader = new FXMLLoader(GuiService.class.getResource("/view/login/initial-config-dialog.fxml"));
                Parent root = loader.load();
                currentController = loader.getController();

                Scene scene = new Scene(root);
                UiUtils.setCommonCss(scene);
                primaryStage.setTitle("Initial configuration");
                primaryStage.setScene(scene);
                primaryStage.show();
            } catch (IOException ex) {
                throw new IOError(ex);
            }
        });
    }

    public void showEncryptionPasswordDialog() {
        Platform.runLater(() -> {
            try {
                if (currentController instanceof EncryptionPasswordController) {
                    return;
                }
                logger.debug("Showing encryption password dialog");
                FXMLLoader loader = new FXMLLoader(GuiService.class.getResource("/view/login/encryption-password-dialog.fxml"));
                Parent root = loader.load();
                currentController = loader.getController();

                Scene scene = new Scene(root);
                UiUtils.setCommonCss(scene);
                primaryStage.setTitle("Encryption password");
                primaryStage.setScene(scene);
                primaryStage.show();
            } catch (IOException ex) {
                throw new IOError(ex);
            }
        });
    }

    public void showPhoneNumberDialog() {
        Platform.runLater(() -> {
            try {
                if (currentController instanceof PhoneDialogController) {
                    return;
                }
                logger.debug("Showing phone number dialog");
                FXMLLoader loader = new FXMLLoader(GuiService.class.getResource("/view/login/phone-dialog.fxml"));
                Parent root = loader.load();
                currentController = loader.getController();

                Scene scene = new Scene(root);
                UiUtils.setCommonCss(scene);
                primaryStage.setTitle("Phone number");
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
                if (currentController instanceof AuthenticationCodeController) {
                    return;
                }
                logger.debug("Showing authentication code dialog");
                FXMLLoader loader = new FXMLLoader(GuiService.class.getResource("/view/login/authentication-code-dialog.fxml"));
                Parent root = loader.load();
                currentController = loader.getController();

                Scene scene = new Scene(root);
                UiUtils.setCommonCss(scene);
                primaryStage.setTitle("Authentication code");
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
                if (currentController instanceof AuthenticationPasswordController) {
                    return;
                }
                logger.debug("Showing authentication password dialog");
                FXMLLoader loader = new FXMLLoader(GuiService.class.getResource("/view/login/authentication-password-dialog.fxml"));
                Parent root = loader.load();
                currentController = loader.getController();

                Scene scene = new Scene(root);
                UiUtils.setCommonCss(scene);
                primaryStage.setTitle("Authentication password");
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
                if (currentController instanceof MainWindowController) {
                    return;
                }
                logger.debug("Showing main window");
                FXMLLoader loader = new FXMLLoader(GuiService.class.getResource("/view/main-window.fxml"));
                Parent root = loader.load();
                currentController = loader.getController();

                Scene scene = new Scene(root);
                UiUtils.setCommonCss(scene);

                scene.widthProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) -> {
                    publish(new WindowWidthChanged(newSceneWidth.intValue()));
                });

                scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
                    if (event.getCode() == KeyCode.ESCAPE) { // Detect escape key pressed
                        // hide chat
                        var chatService = ServiceLocator.getService(ChatsService.class);
                        chatService.setSelectedChat(null);
                    } else if (event.isControlDown() && event.getCode() == KeyCode.C) { // Detect Ctrl+C
                        if (currentSelectable != null) {
                            Clipboard clipboard = Clipboard.getSystemClipboard();
                            ClipboardContent content = new ClipboardContent();
                            content.putString(currentSelectable.getSelectedText());
                            clipboard.setContent(content);
                        }
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
        List<EventListener> listeners = eventListeners.get(event.getClass());
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
        logger.debug("Subscribing to event {}", eventType.getSimpleName());
        eventListeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    private Image createLockImage() {
        try (InputStream in = ChatsListView.class.getResourceAsStream("/view/icons/icons8-lock.png")) {
            return new Image(in);
        } catch (IOException ex) {
            logger.error("Unable to initialize lock image", ex);
        }
        return null;
    }

    public Image getLockImage() {
        return lockImage;
    }

    public void showDocument(String url) {
        app.getHostServices().showDocument(url);
    }

    public void setCurrentSelectable(SelectableText selectableText) {
        if (currentSelectable != null) {
            currentSelectable.resetSelection();
        }
        currentSelectable = selectableText;
    }
}
