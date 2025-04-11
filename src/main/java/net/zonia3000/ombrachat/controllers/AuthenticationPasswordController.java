package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.services.TelegramClientService;

public class AuthenticationPasswordController implements ErrorHandlerController {

    @FXML
    private PasswordField authenticationPasswordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button nextBtn;

    @FXML
    private void initialize() {
        UiUtils.setVisible(errorLabel, false);
        nextBtn.setDisable(false);
    }

    @FXML
    private void next() {
        displayError("");
        String password = authenticationPasswordField.getText();
        if (password.isBlank()) {
            return;
        }
        nextBtn.setDisable(true);
        var clientService = ServiceLocator.getService(TelegramClientService.class);
        clientService.setAuthenticationPassword(password);
    }

    @Override
    public void displayError(String error) {
        nextBtn.setDisable(false);
        UiUtils.setVisible(errorLabel, !error.isBlank());
        errorLabel.setText(error);
    }
}
