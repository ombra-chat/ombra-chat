package net.zonia3000.ombrachat.login;

import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.zonia3000.ombrachat.ErrorHandlerController;

public class AuthenticationCodeController implements ErrorHandlerController {
    
    @FXML
    private TextField authenticationCodeTextField;
    @FXML
    private Label errorLabel;

    private Consumer<String> authenticationCodeConsumer;

    public void setAuthenticationCodeConsumer(Consumer<String> phoneNumberConsumer) {
        this.authenticationCodeConsumer = phoneNumberConsumer;
    }

    @FXML
    private void handleNextButtonClick() {
        errorLabel.setText("");
        String authenticationCode = authenticationCodeTextField.getText();
        if (authenticationCode.trim().equals("")) {
            return;
        }
        authenticationCodeConsumer.accept(authenticationCode);
    }

    @Override
    public void displayError(String error) {
        errorLabel.setText(error);
    }
}
