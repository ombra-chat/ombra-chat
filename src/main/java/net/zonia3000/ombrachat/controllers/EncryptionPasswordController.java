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
    private Label encryptionPasswordLabel;
    @FXML
    private PasswordField encryptionPasswordField;
    @FXML
    private Label gpgPassphraseLabel;
    @FXML
    private PasswordField gpgPassphraseField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button nextBtn;

    private final SettingsService settings;
    private final GpgService gpgService;
    private final UserService userService;

    public EncryptionPasswordController() {
        settings = ServiceLocator.getService(SettingsService.class);
        gpgService = ServiceLocator.getService(GpgService.class);
        userService = ServiceLocator.getService(UserService.class);
    }

    @FXML
    private void initialize() {
        var showEncryptionPasswordField = settings.getTdlibDatabaseEncryption() == SettingsService.EncryptionType.PASSWORD;
        UiUtils.setVisible(encryptionPasswordLabel, showEncryptionPasswordField);
        UiUtils.setVisible(encryptionPasswordField, showEncryptionPasswordField);

        UiUtils.setVisible(gpgPassphraseLabel, gpgService.hasPrivateKey());
        UiUtils.setVisible(gpgPassphraseField, gpgService.hasPrivateKey());

        UiUtils.setVisible(errorLabel, false);
    }

    @FXML
    private void next() {
        displayError("");
        if (settings.getTdlibDatabaseEncryption() == SettingsService.EncryptionType.PASSWORD) {
            String password = encryptionPasswordField.getText();
            if (password.isBlank()) {
                return;
            }
            userService.setEncryptionPassword(password);
        }
        if (gpgService.hasPrivateKey()) {
            char[] passphrase = gpgPassphraseField.getText().toCharArray();
            if (!gpgService.checkSecretKey(passphrase)) {
                displayError("Wrong GPG passphrase");
                return;
            }
        }
        if (settings.getTdlibDatabaseEncryption() == SettingsService.EncryptionType.GPG) {
            var password = gpgService.decryptText(settings.getTdlibEncryptedPassword());
            if (password == null) {
                displayError("Error decrypting Telegram password");
                return;
            }
            userService.setEncryptionPassword(password);
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
