package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.services.TelegramClientService;

public class AuthenticationCodeController implements ErrorHandlerController {

    @FXML
    private TextField authenticationCodeTextField;
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
        String authenticationCode = authenticationCodeTextField.getText();
        if (authenticationCode.isBlank()) {
            return;
        }
        nextBtn.setDisable(true);
        var clientService = ServiceLocator.getService(TelegramClientService.class);
        clientService.setAuthenticationCode(authenticationCode);
    }

    @Override
    public void displayError(String error) {
        nextBtn.setDisable(false);
        UiUtils.setVisible(errorLabel, !error.isBlank());
        errorLabel.setText(error);
    }
}
