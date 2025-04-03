package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.services.TelegramClientService;

public class PhoneDialogController implements ErrorHandlerController {

    @FXML
    private TextField phoneTextField;
    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleNextButtonClick() {
        displayError("");
        String phoneNumber = phoneTextField.getText();
        if (phoneNumber.isBlank()) {
            return;
        }
        var clientService = ServiceLocator.getService(TelegramClientService.class);
        clientService.setPhoneNumber(phoneNumber);
    }

    @Override
    public void displayError(String error) {
        errorLabel.setManaged(!error.isBlank());
        errorLabel.setText(error);
    }
}
