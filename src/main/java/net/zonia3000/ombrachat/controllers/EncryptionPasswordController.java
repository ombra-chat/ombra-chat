package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.services.SettingsService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import net.zonia3000.ombrachat.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionPasswordController implements ErrorHandlerController {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionPasswordController.class);

    @FXML
    private PasswordField encryptionPasswordField;
    @FXML
    private PasswordField gpgPassphraseField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button nextBtn;

    private final SettingsService settings;
    private final GpgService gpgService;

    public EncryptionPasswordController() {
        settings = ServiceLocator.getService(SettingsService.class);
        gpgService = ServiceLocator.getService(GpgService.class);
    }

    @FXML
    private void initialize() {
        UiUtils.setVisible(encryptionPasswordField, settings.isTdlibDatabaseEncrypted());
        UiUtils.setVisible(gpgPassphraseField, gpgService.hasPrivateKey());

        UiUtils.setVisible(errorLabel, false);
    }

    @FXML
    private void handleNextButtonClick() {
        displayError("");
        if (settings.isTdlibDatabaseEncrypted()) {
            String password = encryptionPasswordField.getText();
            if (password.isBlank()) {
                return;
            }
            var userService = ServiceLocator.getService(UserService.class);
            userService.setEncryptionPassword(password);
        }
        if (gpgService.hasPrivateKey()) {
            char[] passphrase = gpgPassphraseField.getText().toCharArray();
            if (gpgService.checkSecretKey(passphrase)) {
                gpgService.setPassphrase(passphrase);
            } else {
                displayError("Wrong GPG passphrase");
                return;
            }
        }
        nextBtn.setDisable(true);
        var clientService = ServiceLocator.getService(TelegramClientService.class);
        clientService.startClient();
    }

    @Override
    public void displayError(String error) {
        UiUtils.setVisible(errorLabel, !error.isBlank());
        errorLabel.setText(error);
        nextBtn.setDisable(false);
    }
}
