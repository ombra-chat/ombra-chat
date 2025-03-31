package net.zonia3000.ombrachat.login;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.zonia3000.ombrachat.ErrorHandlerController;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.services.TelegramClientService;

public class PhoneDialogController implements ErrorHandlerController {

    @FXML
    private TextField phoneTextField;
    @FXML
    private Label errorLabel;

    @FXML
    private void handleNextButtonClick() {
        errorLabel.setText("");
        String phoneNumber = phoneTextField.getText();
        if (phoneNumber.trim().equals("")) {
            return;
        }
        var clientService = ServiceLocator.getService(TelegramClientService.class);
        clientService.setPhoneNumber(phoneNumber);
    }

    @Override
    public void displayError(String error) {
        errorLabel.setText(error);
    }
}
