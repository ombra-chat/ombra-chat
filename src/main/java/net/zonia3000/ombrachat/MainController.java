package net.zonia3000.ombrachat;

import java.io.IOError;
import java.io.IOException;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import net.zonia3000.ombrachat.login.AuthenticationCodeController;
import net.zonia3000.ombrachat.login.AuthenticationPasswordController;
import net.zonia3000.ombrachat.login.PhoneDialogController;

public class MainController {

    private final Stage primaryStage;

    private ErrorHandlerController currentController;

    public MainController(Stage stage) {
        this.primaryStage = stage;
    }

    public void displayError(String errorMessage) {
        Platform.runLater(() -> {
            System.err.println(errorMessage);
            if (currentController == null) {
                return;
            }
            currentController.displayError(errorMessage);
        });
    }

    public void showPhoneNumberDialog(Consumer<String> phoneNumberConsumer) {
        Platform.runLater(() -> {
            if (currentController != null && currentController instanceof PhoneDialogController controller) {
                controller.setPhoneNumberConsumer(phoneNumberConsumer);
            } else {
                try {
                    FXMLLoader loader = new FXMLLoader();

                    loader.setLocation(MainController.class.getResource("/view/login/phone-dialog.fxml"));
                    Parent root = loader.load();
                    PhoneDialogController controller = loader.getController();
                    currentController = controller;
                    controller.setPhoneNumberConsumer(phoneNumberConsumer);

                    Scene scene = new Scene(root);
                    primaryStage.setScene(scene);
                    primaryStage.show();
                } catch (IOException ex) {
                    throw new IOError(ex);
                }
            }
        });
    }

    public void showAuthenticationCodeDialog(Consumer<String> authenticationCodeConsumer) {
        Platform.runLater(() -> {
            if (currentController != null && currentController instanceof AuthenticationCodeController controller) {
                controller.setAuthenticationCodeConsumer(authenticationCodeConsumer);
            } else {
                try {
                    FXMLLoader loader = new FXMLLoader();

                    loader.setLocation(MainController.class.getResource("/view/login/authentication-code-dialog.fxml"));
                    Parent root = loader.load();
                    AuthenticationCodeController controller = loader.getController();
                    currentController = controller;
                    controller.setAuthenticationCodeConsumer(authenticationCodeConsumer);

                    Scene scene = new Scene(root);
                    primaryStage.setScene(scene);
                    primaryStage.show();
                } catch (IOException ex) {
                    throw new IOError(ex);
                }
            }
        });
    }

    public void showAuthenticationPasswordDialog(Consumer<String> passwordConsumer) {
        Platform.runLater(() -> {
            if (currentController != null && currentController instanceof AuthenticationPasswordController controller) {
                controller.setPasswordConsumer(passwordConsumer);
            } else {
                try {
                    FXMLLoader loader = new FXMLLoader();

                    loader.setLocation(MainController.class.getResource("/view/login/authentication-password-dialog.fxml"));
                    Parent root = loader.load();
                    AuthenticationPasswordController controller = loader.getController();
                    currentController = controller;
                    controller.setPasswordConsumer(passwordConsumer);

                    Scene scene = new Scene(root);
                    primaryStage.setScene(scene);
                    primaryStage.show();
                } catch (IOException ex) {
                    throw new IOError(ex);
                }
            }
        });
    }

    public void showMainWindow(ChatsLoader chatsLoader) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader();

                loader.setLocation(MainController.class.getResource("/view/main-window.fxml"));
                Parent root = loader.load();
                MainWindowController controller = loader.getController();
                currentController = controller;
                controller.setChatsLoader(chatsLoader);

                Scene scene = new Scene(root);

                scene.widthProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) -> {
                    controller.setWindowWidth(newSceneWidth.intValue());
                });

                primaryStage.setScene(scene);
                primaryStage.show();

            } catch (IOException ex) {
                throw new IOError(ex);
            }
        });
    }
}
