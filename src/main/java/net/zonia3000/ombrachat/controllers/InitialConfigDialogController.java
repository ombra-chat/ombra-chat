package net.zonia3000.ombrachat.controllers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.services.GuiService;
import net.zonia3000.ombrachat.services.SettingsService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import net.zonia3000.ombrachat.services.UserService;
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
    private CheckBox encryptTdlib;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {
        var guiService = ServiceLocator.getService(GuiService.class);
        myTelegramLink.setOnAction(event -> guiService.showDocument("https://my.telegram.org"));
        appFolderTextField.setText(getDefaultAppFolderPath());
        UiUtils.setVisible(passwordField, false);
        UiUtils.setVisible(errorLabel, false);
    }

    private String getDefaultAppFolderPath() {
        String homeDir = System.getProperty("user.home");
        Path dir = Paths.get(homeDir, ".ombra-chat");
        return dir.toAbsolutePath().toString();
    }

    @FXML
    public void handleEncryptCheckboxSelection() {
        UiUtils.setVisible(passwordField, encryptTdlib.isSelected());
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

        var settings = ServiceLocator.getService(SettingsService.class);
        try {
            int apiId = Integer.parseInt(apiIdString);
            settings.setApiId(apiId);
            settings.setApiHash(apiHash);
        } catch (NumberFormatException ex) {
            displayError("Invalid API ID format: should be a number");
            return;
        }

        settings.setTdlibDatabaseEncrypted(encryptTdlib.isSelected());

        if (encryptTdlib.isSelected()) {
            if (passwordField.getText().isBlank()) {
                displayError("Password is required");
                return;
            }
            ServiceLocator.getService(UserService.class).setEncryptionPassword(passwordField.getText());
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
