package net.zonia3000.ombrachat.login;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import net.zonia3000.ombrachat.ErrorHandlerController;
import net.zonia3000.ombrachat.Mediator;
import net.zonia3000.ombrachat.events.AuthenticationPasswordSet;

public class AuthenticationPasswordController implements ErrorHandlerController {

    @FXML
    private PasswordField authenticationPasswordField;
    @FXML
    private Label errorLabel;

    private Mediator mediator;

    public void setMediator(Mediator mediator) {
        this.mediator = mediator;
    }

    @FXML
    private void handleNextButtonClick() {
        errorLabel.setText("");
        String password = authenticationPasswordField.getText();
        if (password.trim().equals("")) {
            return;
        }
        mediator.publish(new AuthenticationPasswordSet(password));
    }

    @Override
    public void displayError(String error) {
        errorLabel.setText(error);
    }
}
