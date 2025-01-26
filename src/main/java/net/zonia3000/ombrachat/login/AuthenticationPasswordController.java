package net.zonia3000.ombrachat.login;

import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import net.zonia3000.ombrachat.ErrorHandlerController;

public class AuthenticationPasswordController implements ErrorHandlerController {

    @FXML
    private PasswordField authenticationPasswordField;
    @FXML
    private Label errorLabel;

    private Consumer<String> passwordConsumer;

    public void setPasswordConsumer(Consumer<String> passwordConsumer) {
        this.passwordConsumer = passwordConsumer;
    }

    @FXML
    private void handleNextButtonClick() {
        errorLabel.setText("");
        String password = authenticationPasswordField.getText();
        if (password.trim().equals("")) {
            return;
        }
        passwordConsumer.accept(password);
    }

    @Override
    public void displayError(String error) {
        errorLabel.setText(error);
    }
}
