package net.zonia3000.ombrachat;

import java.io.IOError;
import java.io.IOException;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
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
}
