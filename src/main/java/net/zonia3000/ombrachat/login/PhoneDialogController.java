package net.zonia3000.ombrachat.login;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.zonia3000.ombrachat.ErrorHandlerController;
import net.zonia3000.ombrachat.Mediator;
import net.zonia3000.ombrachat.events.PhoneNumberSet;

public class PhoneDialogController implements ErrorHandlerController {

    @FXML
    private TextField phoneTextField;
    @FXML
    private Label errorLabel;

    private Mediator mediator;

    public void setMediator(Mediator mediator) {
        this.mediator = mediator;
    }

    @FXML
    private void handleNextButtonClick() {
        errorLabel.setText("");
        String phoneNumber = phoneTextField.getText();
        if (phoneNumber.trim().equals("")) {
            return;
        }
        mediator.publish(new PhoneNumberSet(phoneNumber));
    }

    @Override
    public void displayError(String error) {
        errorLabel.setText(error);
    }
}
