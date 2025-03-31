package net.zonia3000.ombrachat.chat;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.GpgUtils;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.events.ChatSettingsSaved;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.GuiService;
import net.zonia3000.ombrachat.services.SettingsService;

public class ChatSettingsDialogController {

    @FXML
    private ComboBox<GpgUtils.GpgPublicKey> keysComboBox;
    @FXML
    private CheckBox enableGPGCheckBox;
    @FXML
    private Button saveBtn;

    private final SettingsService settings;
    private final ChatsService chatsService;
    private final GuiService guiService;

    public ChatSettingsDialogController() {
        this.settings = ServiceLocator.getService(SettingsService.class);
        this.chatsService = ServiceLocator.getService(ChatsService.class);
        this.guiService = ServiceLocator.getService(GuiService.class);
    }

    @FXML
    public void initialize() {
        var keys = GpgUtils.listKeys();
        for (var k : keys) {
            keysComboBox.getItems().add(k);
        }
        String chatKeyFingerprint = settings.getChatKey(chatsService.getSelectedChat().id);
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
        if (enableGPGCheckBox.isSelected()) {
            var selected = keysComboBox.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            settings.setChatKey(selectedChat.id, selected.getFingerprint());
        } else {
            settings.setChatKey(selectedChat.id, null);
        }

        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
        guiService.publish(new ChatSettingsSaved());
    }
}
