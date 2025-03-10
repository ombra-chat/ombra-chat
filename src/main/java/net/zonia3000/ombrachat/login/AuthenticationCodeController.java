package net.zonia3000.ombrachat.login;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.zonia3000.ombrachat.ErrorHandlerController;
import net.zonia3000.ombrachat.Mediator;
import net.zonia3000.ombrachat.events.AuthenticationCodeSet;

public class AuthenticationCodeController implements ErrorHandlerController {

    @FXML
    private TextField authenticationCodeTextField;
    @FXML
    private Label errorLabel;

    private Mediator mediator;

    public void setMediator(Mediator mediator) {
        this.mediator = mediator;
    }

    @FXML
    private void handleNextButtonClick() {
        errorLabel.setText("");
        String authenticationCode = authenticationCodeTextField.getText();
        if (authenticationCode.trim().equals("")) {
            return;
        }
        mediator.publish(new AuthenticationCodeSet(authenticationCode));
    }

    @Override
    public void displayError(String error) {
        errorLabel.setText(error);
    }
}
