package net.zonia3000.ombrachat.login;

import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.zonia3000.ombrachat.ErrorHandlerController;

public class PhoneDialogController implements ErrorHandlerController {

    @FXML
    private TextField phoneTextField;
    @FXML
    private Label errorLabel;

    private Consumer<String> phoneNumberConsumer;

    public void setPhoneNumberConsumer(Consumer<String> phoneNumberConsumer) {
        this.phoneNumberConsumer = phoneNumberConsumer;
    }

    @FXML
    private void handleNextButtonClick() {
        errorLabel.setText("");
        String phoneNumber = phoneTextField.getText();
        if (phoneNumber.trim().equals("")) {
            return;
        }
        phoneNumberConsumer.accept(phoneNumber);
    }

    @Override
    public void displayError(String error) {
        errorLabel.setText(error);
    }
}
