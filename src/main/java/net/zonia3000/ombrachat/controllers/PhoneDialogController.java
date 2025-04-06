package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.services.TelegramClientService;

public class PhoneDialogController implements ErrorHandlerController {

    @FXML
    private TextField phoneTextField;
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
    private void handleNextButtonClick() {
        displayError("");
        String phoneNumber = phoneTextField.getText();
        if (phoneNumber.isBlank()) {
            return;
        }
        nextBtn.setDisable(true);
        var clientService = ServiceLocator.getService(TelegramClientService.class);
        clientService.setPhoneNumber(phoneNumber);
    }

    @Override
    public void displayError(String error) {
        nextBtn.setDisable(false);
        UiUtils.setVisible(errorLabel, !error.isBlank());
        errorLabel.setText(error);
    }
}
