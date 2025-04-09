package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.events.ChatSettingsSaved;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.GuiService;
import net.zonia3000.ombrachat.services.SettingsService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import org.drinkless.tdlib.TdApi;

public class ChatSettingsDialogController {

    @FXML
    private ComboBox<GpgService.GpgPublicKey> keysComboBox;
    @FXML
    private CheckBox enableGPGCheckBox;
    @FXML
    private Button secretChatBtn;
    @FXML
    private Button saveBtn;

    private boolean showGpgSettings;

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

        UiUtils.setVisible(keysComboBox, showGpgSettings);
        UiUtils.setVisible(enableGPGCheckBox, showGpgSettings);
        UiUtils.setVisible(secretChatBtn, selectedChat.type instanceof TdApi.ChatTypePrivate);

        if (!showGpgSettings) {
            return;
        }

        var keys = gpgService.listKeys();
        for (var k : keys) {
            keysComboBox.getItems().add(k);
        }
        String chatKeyFingerprint = settings.getChatKeyFingerprint(selectedChat.id);
        if (chatKeyFingerprint != null) {
            var key = keys.stream().filter(k -> k.getFingerprint().equals(chatKeyFingerprint)).findFirst();
            if (key.isPresent()) {
                enableGPGCheckBox.setSelected(true);
                keysComboBox.getSelectionModel().select(key.get());
            }
        }
    }

    @FXML
    private void handleSaveButtonClick() {
        var selectedChat = chatsService.getSelectedChat();

        if (showGpgSettings) {
            if (enableGPGCheckBox.isSelected()) {
                var selected = keysComboBox.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    return;
                }
                settings.setChatKeyFingerprint(selectedChat.id, selected.getFingerprint());
            } else {
                settings.setChatKeyFingerprint(selectedChat.id, null);
            }
        }

        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
        guiService.publish(new ChatSettingsSaved());
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
