package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.services.TelegramClientService;
import net.zonia3000.ombrachat.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionPasswordController implements ErrorHandlerController {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionPasswordController.class);

    @FXML
    private PasswordField encryptionPasswordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button nextBtn;

    @FXML
    private void initialize() {
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleNextButtonClick() {
        displayError("");
        String password = encryptionPasswordField.getText();
        if (password.isBlank()) {
            return;
        }
        nextBtn.setDisable(true);
        var userService = ServiceLocator.getService(UserService.class);
        var clientService = ServiceLocator.getService(TelegramClientService.class);
        userService.setEncryptionPassword(password);
        clientService.startClient();
    }

    @Override
    public void displayError(String error) {
        errorLabel.setManaged(!error.isBlank());
        errorLabel.setText(error);
        nextBtn.setDisable(false);
    }
}
