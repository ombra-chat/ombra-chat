package net.zonia3000.ombrachat.controllers;

import java.nio.file.Files;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.services.SettingsService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import net.zonia3000.ombrachat.services.CurrentUserService;
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
    private Label newPrivateKeyDetected;
    @FXML
    private Label gpgPassphraseLabelOld;
    @FXML
    private PasswordField gpgPassphraseOldField;

    @FXML
    private Label errorLabel;
    @FXML
    private Button nextBtn;

    private final SettingsService settings;
    private final GpgService gpgService;
    private final CurrentUserService currentUserService;

    private boolean replacePrivateKey;
    private boolean replaced;
    private char[] tdlibPassword;

    public EncryptionPasswordController() {
        settings = ServiceLocator.getService(SettingsService.class);
        gpgService = ServiceLocator.getService(GpgService.class);
        currentUserService = ServiceLocator.getService(CurrentUserService.class);
    }

    @FXML
    private void initialize() {
        var showEncryptionPasswordField = settings.getTdlibDatabaseEncryption() == SettingsService.EncryptionType.PASSWORD;
        UiUtils.setVisible(encryptionPasswordLabel, showEncryptionPasswordField);
        UiUtils.setVisible(encryptionPasswordField, showEncryptionPasswordField);

        UiUtils.setVisible(gpgPassphraseLabel, gpgService.hasPrivateKey());
        UiUtils.setVisible(gpgPassphraseField, gpgService.hasPrivateKey());

        UiUtils.setVisible(errorLabel, false);

        var newKeyPath = gpgService.getGpgDirectoryPath().resolve("private.asc.new");
        replacePrivateKey = gpgService.hasPrivateKey()
                && settings.getTdlibDatabaseEncryption() == SettingsService.EncryptionType.GPG
                && Files.exists(newKeyPath);

        UiUtils.setVisible(newPrivateKeyDetected, replacePrivateKey);
        UiUtils.setVisible(gpgPassphraseLabelOld, replacePrivateKey);
        UiUtils.setVisible(gpgPassphraseOldField, replacePrivateKey);

        UiUtils.setupMiddleButtonPasteHandler(encryptionPasswordField);
        UiUtils.setupMiddleButtonPasteHandler(gpgPassphraseField);
        UiUtils.setupMiddleButtonPasteHandler(gpgPassphraseOldField);
    }

    @FXML
    private void next() {
        displayError("");
        nextBtn.setDisable(true);
        if (replacePrivateKey && !replacePrivateKey()) {
            return;
        }
        if (settings.getTdlibDatabaseEncryption() == SettingsService.EncryptionType.PASSWORD) {
            String password = encryptionPasswordField.getText();
            if (password.isBlank()) {
                displayError("Password is required");
                return;
            }
            currentUserService.setEncryptionPassword(password);
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
            currentUserService.setEncryptionPassword(password);
        }
        var clientService = ServiceLocator.getService(TelegramClientService.class);
        clientService.startClient();
    }

    private boolean replacePrivateKey() {
        if (!replaced) {
            char[] oldPassphrase = gpgPassphraseOldField.getText().toCharArray();
            if (!gpgService.checkSecretKey(oldPassphrase)) {
                displayError("Wrong old GPG passphrase");
                return false;
            }

            var password = gpgService.decryptText(settings.getTdlibEncryptedPassword());
            if (password == null) {
                displayError("Error decrypting Telegram password");
                return false;
            }
            tdlibPassword = password.toCharArray();

            var oldKeyPath = gpgService.getGpgDirectoryPath().resolve("private.asc");
            var newKeyPath = gpgService.getGpgDirectoryPath().resolve("private.asc.new");

            oldKeyPath.toFile().renameTo(oldKeyPath.getParent().resolve("private.asc.old").toFile());
            newKeyPath.toFile().renameTo(newKeyPath.getParent().resolve("private.asc").toFile());
            logger.debug("Private key replaced");
            replaced = true;
        }

        char[] newPassphrase = gpgPassphraseField.getText().toCharArray();
        if (!gpgService.checkSecretKey(newPassphrase)) {
            displayError("Wrong new GPG passphrase");
            return false;
        }

        var newEncryptedPassword = gpgService.encryptText(new String(tdlibPassword));
        if (newEncryptedPassword == null) {
            displayError("Error encrypting Telegram password with new key");
            return false;
        }
        logger.debug("Telegram encryption password replaced");
        settings.setTdlibEncryptedPassword(newEncryptedPassword);

        tdlibPassword = null;
        return true;
    }

    @Override
    public void displayError(String error) {
        UiUtils.setVisible(errorLabel, !error.isBlank());
        errorLabel.setText(error);
        nextBtn.setDisable(false);
    }
}
