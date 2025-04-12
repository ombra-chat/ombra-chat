package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiException;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.services.ChatsService;
import static net.zonia3000.ombrachat.services.GpgService.bytesToHex;
import net.zonia3000.ombrachat.services.GuiService;
import net.zonia3000.ombrachat.services.SettingsService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatSettingsDialogController {

    private static final Logger logger = LoggerFactory.getLogger(ChatSettingsDialogController.class);

    @FXML
    private CheckBox enableGPGCheckBox;
    @FXML
    private RadioButton keySourcePubring;
    @FXML
    private RadioButton keySourceFile;
    @FXML
    private ComboBox<GpgService.GpgPublicKey> keysComboBox;
    @FXML
    private ComboBox<String> encryptionKeysComboBox;
    @FXML
    private Label errorLabel;
    @FXML
    private Label encryptionKeyLabel;
    @FXML
    private Button selectKeyBtn;
    @FXML
    private Button saveBtn;
    @FXML
    private Button secretChatBtn;

    private boolean showGpgSettings;
    private GpgService.GpgPublicKey selectedKey;
    private String pubringError;

    private final SettingsService settings;
    private final ChatsService chatsService;
    private final GuiService guiService;
    private final GpgService gpgService;
    private final TelegramClientService clientService;

    public ChatSettingsDialogController() {
        this.settings = ServiceLocator.getService(SettingsService.class);
        this.chatsService = ServiceLocator.getService(ChatsService.class);
        this.guiService = ServiceLocator.getService(GuiService.class);
        this.gpgService = ServiceLocator.getService(GpgService.class);
        this.clientService = ServiceLocator.getService(TelegramClientService.class);
    }

    @FXML
    public void initialize() {
        var selectedChat = chatsService.getSelectedChat();
        showGpgSettings = selectedChat.type instanceof TdApi.ChatTypePrivate
                || selectedChat.type instanceof TdApi.ChatTypeSecret;

        UiUtils.setVisible(enableGPGCheckBox, showGpgSettings);
        UiUtils.setVisible(secretChatBtn, selectedChat.type instanceof TdApi.ChatTypePrivate);

        if (showGpgSettings) {
            try {
                var keys = gpgService.listKeys();
                for (var k : keys) {
                    keysComboBox.getItems().add(k);
                }
                String chatKeyFingerprint = settings.getChatKeyFingerprint(selectedChat.id);
                if (chatKeyFingerprint != null) {
                    for (var k : keys) {
                        var key = gpgService.getKeyFromFingerprint(k, chatKeyFingerprint);
                        if (key != null) {
                            keysComboBox.getSelectionModel().select(k);
                            enableGPGCheckBox.setSelected(true);
                            setSelectedKey(key);
                            break;
                        }
                    }
                }
            } catch (UiException ex) {
                pubringError = ex.getMessage();
                setErrorLabel(pubringError);
            }
        }

        setGpgComponentsVisibility();
    }

    @FXML
    private void toggleGPG() {
        setGpgComponentsVisibility();
    }

    @FXML
    private void keySourceChanged() {
        setSelectedKey(null);
        setKeySourceVisibility();
    }

    private void setGpgComponentsVisibility() {
        var enabled = enableGPGCheckBox.isSelected();
        UiUtils.setVisible(keySourcePubring, enabled);
        UiUtils.setVisible(keySourceFile, enabled);
        setKeySourceVisibility();
    }

    private void setKeySourceVisibility() {
        var enabled = enableGPGCheckBox.isSelected();
        if (keySourcePubring.isSelected()) {
            setErrorLabel(pubringError);
        }
        UiUtils.setVisible(keysComboBox, enabled && keySourcePubring.isSelected());
        UiUtils.setVisible(encryptionKeyLabel, enabled);
        UiUtils.setVisible(encryptionKeysComboBox, enabled && !encryptionKeysComboBox.getItems().isEmpty());
        UiUtils.setVisible(selectKeyBtn, enabled && keySourceFile.isSelected());
        UiUtils.setVisible(errorLabel, enabled && errorLabel.getText() != null && !errorLabel.getText().isBlank());
    }

    @FXML
    private void selectedKeyChanged() {
        setSelectedKey(keysComboBox.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void selectedEncryptionKeyChanged() {
        var selected = encryptionKeysComboBox.getSelectionModel().getSelectedItem();
        if (selectedKey != null && selected != null) {
            selectedKey.setEncryptionKey(selected);
        }
    }

    @FXML
    private void openFileDialog() {
        setErrorLabel("");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select public key file");
        var selectedFile = fileChooser.showOpenDialog(selectKeyBtn.getScene().getWindow());
        if (selectedFile == null) {
            setSelectedKey(null);
        } else {
            var loadedKey = gpgService.loadPublicKeyFromFile(selectedFile.getAbsolutePath());
            if (loadedKey == null) {
                setErrorLabel("Error loading public key from file");
            }
            setSelectedKey(loadedKey);
        }
    }

    private void setSelectedKey(GpgService.GpgPublicKey publicKey) {
        logger.debug("set selected key {}", publicKey);
        selectedKey = publicKey;
        encryptionKeysComboBox.getItems().clear();
        UiUtils.setVisible(encryptionKeysComboBox, publicKey != null);
        if (publicKey == null) {
            keysComboBox.getSelectionModel().clearSelection();
            return;
        }
        for (var keys : publicKey.getAvailableEncryptionKeys()) {
            encryptionKeysComboBox.getItems().add(bytesToHex(keys.getFingerprint()));
        }
        var fingerprint = publicKey.getFingerprint();
        if (fingerprint != null) {
            encryptionKeysComboBox.getSelectionModel().select(fingerprint);
        }
    }

    private void setErrorLabel(String text) {
        errorLabel.setText(text);
        UiUtils.setVisible(errorLabel, text != null && !text.isBlank());
    }

    @FXML
    private void handleSaveButtonClick() {
        var selectedChat = chatsService.getSelectedChat();

        if (showGpgSettings) {
            if (enableGPGCheckBox.isSelected()) {
                if (keySourceFile.isSelected()) {
                    if (selectedKey == null) {
                        return;
                    }
                    if (selectedKey.getFingerprint() == null) {
                        setErrorLabel("Select encryption key");
                    }
                    try {
                        gpgService.saveKeyToFile(selectedKey);
                    } catch (UiException ex) {
                        setErrorLabel(ex.getMessage());
                        return;
                    }
                    settings.setChatKeyFingerprint(selectedChat.id, selectedKey.getFingerprint());
                } else if (keySourcePubring.isSelected()) {
                    var selected = keysComboBox.getSelectionModel().getSelectedItem();
                    if (selected == null) {
                        return;
                    }
                    if (selected.getFingerprint() == null) {
                        setErrorLabel("Select encryption key");
                    }
                    try {
                        gpgService.saveKeyToFile(selected);
                    } catch (UiException ex) {
                        setErrorLabel(ex.getMessage());
                        return;
                    }
                    settings.setChatKeyFingerprint(selectedChat.id, selected.getFingerprint());
                }
            } else {
                settings.setChatKeyFingerprint(selectedChat.id, null);
            }
        }

        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();

        var chatPageController = guiService.getController(ChatPageController.class);
        if (chatPageController != null) {
            chatPageController.setGpgKeyLabel();
        }
    }

    @FXML
    private void createNewSecretChat() {
        var selectedChat = chatsService.getSelectedChat();
        if (selectedChat.type instanceof TdApi.ChatTypePrivate chat) {
            clientService.sendClientMessage(new TdApi.CreateNewSecretChat(chat.userId));
            Stage stage = (Stage) saveBtn.getScene().getWindow();
            stage.close();
        }
    }
}
