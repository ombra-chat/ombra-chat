package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.services.TelegramClientService;

public class AuthenticationCodeController implements ErrorHandlerController {

    @FXML
    private TextField authenticationCodeTextField;
    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleNextButtonClick() {
        displayError("");
        String authenticationCode = authenticationCodeTextField.getText();
        if (authenticationCode.isBlank()) {
            return;
        }
        var clientService = ServiceLocator.getService(TelegramClientService.class);
        clientService.setAuthenticationCode(authenticationCode);
    }

    @Override
    public void displayError(String error) {
        errorLabel.setManaged(!error.isBlank());
        errorLabel.setText(error);
    }
}
