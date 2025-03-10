package net.zonia3000.ombrachat;

import java.io.IOError;
import java.io.IOException;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import net.zonia3000.ombrachat.events.ChatSelected;
import net.zonia3000.ombrachat.events.ErrorReceived;
import net.zonia3000.ombrachat.events.LoadChats;
import net.zonia3000.ombrachat.events.ShowAuthenticationCodeDialog;
import net.zonia3000.ombrachat.events.ShowAuthenticationPasswordDialog;
import net.zonia3000.ombrachat.events.ShowPhoneNumberDialog;
import net.zonia3000.ombrachat.events.WindowWidthChanged;
import net.zonia3000.ombrachat.login.AuthenticationCodeController;
import net.zonia3000.ombrachat.login.AuthenticationPasswordController;
import net.zonia3000.ombrachat.login.PhoneDialogController;

public class MainController {

    private final Mediator mediator;
    private final Stage primaryStage;

    private ErrorHandlerController currentController;

    public MainController(Mediator mediator, Stage primaryStage) {
        this.mediator = mediator;
        this.primaryStage = primaryStage;

        mediator.subscribe(ShowPhoneNumberDialog.class, (e) -> showPhoneNumberDialog());
        mediator.subscribe(ShowAuthenticationCodeDialog.class, (e) -> showAuthenticationCodeDialog());
        mediator.subscribe(ShowAuthenticationPasswordDialog.class, (e) -> showAuthenticationPasswordDialog());
        mediator.subscribe(LoadChats.class, (e) -> showMainWindow());
        mediator.subscribe(ErrorReceived.class, (e) -> displayError(e.getError()));
    }

    private void displayError(String errorMessage) {
        Platform.runLater(() -> {
            System.err.println(errorMessage);
            if (currentController == null) {
                return;
            }
            currentController.displayError(errorMessage);
        });
    }

    private void showPhoneNumberDialog() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader();

                loader.setLocation(MainController.class.getResource("/view/login/phone-dialog.fxml"));
                Parent root = loader.load();
                PhoneDialogController controller = loader.getController();
                currentController = controller;
                controller.setMediator(mediator);

                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.show();
            } catch (IOException ex) {
                throw new IOError(ex);
            }
        });
    }

    private void showAuthenticationCodeDialog() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader();

                loader.setLocation(MainController.class.getResource("/view/login/authentication-code-dialog.fxml"));
                Parent root = loader.load();
                AuthenticationCodeController controller = loader.getController();
                currentController = controller;
                controller.setMediator(mediator);

                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.show();
            } catch (IOException ex) {
                throw new IOError(ex);
            }
        });
    }

    private void showAuthenticationPasswordDialog() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader();

                loader.setLocation(MainController.class.getResource("/view/login/authentication-password-dialog.fxml"));
                Parent root = loader.load();
                AuthenticationPasswordController controller = loader.getController();
                currentController = controller;
                controller.setMediator(mediator);

                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.show();
            } catch (IOException ex) {
                throw new IOError(ex);
            }
        });
    }

    private void showMainWindow() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader();

                loader.setLocation(MainController.class.getResource("/view/main-window.fxml"));
                Parent root = loader.load();
                MainWindowController controller = loader.getController();
                currentController = controller;
                controller.setMediator(mediator);

                Scene scene = new Scene(root);
                UiUtils.setCommonCss(scene);

                scene.widthProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) -> {
                    mediator.publish(new WindowWidthChanged(newSceneWidth.intValue()));
                });

                // Detect escape key pressed
                scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        // hide chat
                        mediator.publish(new ChatSelected(null));
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
}
