package net.zonia3000.ombrachat.controllers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import net.zonia3000.ombrachat.CryptoUtils;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.services.GuiService;
import net.zonia3000.ombrachat.services.SettingsService;
import net.zonia3000.ombrachat.services.SettingsService.EncryptionType;
import net.zonia3000.ombrachat.services.TelegramClientService;
import net.zonia3000.ombrachat.services.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitialConfigDialogController implements ErrorHandlerController {

    private static final Logger logger = LoggerFactory.getLogger(InitialConfigDialogController.class);

    @FXML
    private Hyperlink myTelegramLink;

    @FXML
    private TextField apiIdTextField;
    @FXML
    private TextField apiHashTextField;
    @FXML
    private TextField appFolderTextField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label gpgPassphraseLabel;
    @FXML
    private PasswordField gpgPassphraseField;
    @FXML
    private Label errorLabel;
    @FXML
    private Label gpgStatusLabel;
    @FXML
    private Label gpgStatusLabelExplanation;
    @FXML
    private HBox gpgButtonsBox;
    @FXML
    private RadioButton encryptionNone;
    @FXML
    private RadioButton encryptionPassword;
    @FXML
    private RadioButton encryptionGpg;

    private SettingsService settings;
    private GpgService gpgService;
    private CurrentUserService currentUserService;
    private boolean hasGpgKey;
    private EncryptionType selectedEncryptionType = EncryptionType.NONE;
    private boolean ignoreGpg;

    @FXML
    private void initialize() {
        var guiService = ServiceLocator.getService(GuiService.class);
        gpgService = ServiceLocator.getService(GpgService.class);
        currentUserService = ServiceLocator.getService(CurrentUserService.class);
        settings = ServiceLocator.getService(SettingsService.class);
        myTelegramLink.setOnAction(event -> guiService.showDocument("https://my.telegram.org"));
        appFolderTextField.setText(getDefaultAppFolderPath());
        UiUtils.setVisible(passwordField, false);
        UiUtils.setVisible(errorLabel, false);
        UiUtils.setupMiddleButtonPasteHandler(apiIdTextField);
        UiUtils.setupMiddleButtonPasteHandler(apiHashTextField);
        UiUtils.setupMiddleButtonPasteHandler(appFolderTextField);
        UiUtils.setupMiddleButtonPasteHandler(passwordField);
        UiUtils.setupMiddleButtonPasteHandler(gpgPassphraseField);
        checkGpg();
    }

    private String getDefaultAppFolderPath() {
        String homeDir = System.getProperty("user.home");
        Path dir = Paths.get(homeDir, ".ombra-chat");
        return dir.toAbsolutePath().toString();
    }

    @FXML
    private void checkGpg() {
        settings.setApplicationFolderPath(appFolderTextField.getText());
        hasGpgKey = gpgService.hasPrivateKey();
        if (hasGpgKey) {
            gpgStatusLabel.setText("GPG private key found");
            gpgStatusLabel.getStyleClass().add("success");
            UiUtils.setVisible(gpgStatusLabelExplanation, false);
            UiUtils.setVisible(gpgButtonsBox, false);
            UiUtils.setVisible(encryptionGpg, true);
            UiUtils.setVisible(gpgPassphraseField, true);
            UiUtils.setVisible(gpgPassphraseLabel, true);
        } else {
            gpgStatusLabel.setText("GPG private key not found");
            gpgStatusLabel.getStyleClass().add("error");
            gpgStatusLabelExplanation.setText("OmbraChat is searching for a private.asc file in the application folder");
            UiUtils.setVisible(encryptionGpg, false);
            UiUtils.setVisible(gpgPassphraseField, false);
            UiUtils.setVisible(gpgPassphraseLabel, false);
        }
    }

    @FXML
    private void ignoreGpg() {
        ignoreGpg = true;
        UiUtils.setVisible(gpgStatusLabelExplanation, false);
        UiUtils.setVisible(gpgButtonsBox, false);
    }

    @FXML
    private void encryptSelectionChanged() {
        if (encryptionNone.isSelected()) {
            selectedEncryptionType = EncryptionType.NONE;
            UiUtils.setVisible(passwordField, false);
        } else if (encryptionPassword.isSelected()) {
            selectedEncryptionType = EncryptionType.PASSWORD;
            UiUtils.setVisible(passwordField, true);
        } else if (encryptionGpg.isSelected()) {
            selectedEncryptionType = EncryptionType.GPG;
            UiUtils.setVisible(passwordField, false);
        }
    }

    @FXML
    private void handleNextButtonClick() {
        displayError("");

        String apiIdString = apiIdTextField.getText();
        if (apiIdString.isBlank()) {
            displayError("API ID is required");
            return;
        }
        String apiHash = apiHashTextField.getText();
        if (apiHash.isBlank()) {
            displayError("API Hash is required");
            return;
        }

        try {
            int apiId = Integer.parseInt(apiIdString);
            settings.setApiId(apiId);
            settings.setApiHash(apiHash);
        } catch (NumberFormatException ex) {
            displayError("Invalid API ID format: should be a number");
            return;
        }

        if (hasGpgKey || !ignoreGpg) {
            if (gpgPassphraseField.getText().isBlank()) {
                displayError("GPG passphrase is required");
                return;
            }
            if (!gpgService.checkSecretKey(gpgPassphraseField.getText().toCharArray())) {
                displayError("Invalid GPG passphrase");
                return;
            }
        }

        settings.setTdlibDatabaseEncryption(selectedEncryptionType);
        if (selectedEncryptionType == EncryptionType.PASSWORD) {
            if (passwordField.getText().isBlank()) {
                displayError("Password is required");
                return;
            }
            currentUserService.setEncryptionPassword(passwordField.getText());
        } else if (selectedEncryptionType == EncryptionType.GPG) {
            var randomPassword = CryptoUtils.generateRandomPassword();
            var encryptedPassword = gpgService.encryptText(randomPassword);
            if (encryptedPassword == null) {
                displayError("Error encrypting the password");
                return;
            }
            currentUserService.setEncryptionPassword(randomPassword);
            settings.setTdlibEncryptedPassword(encryptedPassword);
        }

        var telegramFolder = appFolderTextField.getText();
        if (!createTelegramParentFolder(telegramFolder)) {
            displayError("Unable to create telegram folder");
            return;
        }
        settings.setApplicationFolderPath(appFolderTextField.getText());

        settings.setInitialConfigDone(true);

        ServiceLocator.getService(TelegramClientService.class).startClient();
    }

    private boolean createTelegramParentFolder(String telegramFolder) {
        var telegramParentFolder = Path.of(telegramFolder).getParent();
        if (Files.exists(telegramParentFolder)) {
            return true;
        }
        return telegramParentFolder.toFile().mkdirs();
    }

    @Override
    public void displayError(String error) {
        UiUtils.setVisible(errorLabel, !error.isBlank());
        errorLabel.setText(error);
    }
}
