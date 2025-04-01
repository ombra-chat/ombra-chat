package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.services.TelegramClientService;

public class AuthenticationPasswordController implements ErrorHandlerController {

    @FXML
    private PasswordField authenticationPasswordField;
    @FXML
    private Label errorLabel;

    @FXML
    private void handleNextButtonClick() {
        errorLabel.setText("");
        String password = authenticationPasswordField.getText();
        if (password.trim().equals("")) {
            return;
        }
        var clientService = ServiceLocator.getService(TelegramClientService.class);
        clientService.setAuthenticationPassword(password);
    }

    @Override
    public void displayError(String error) {
        errorLabel.setText(error);
    }
}
