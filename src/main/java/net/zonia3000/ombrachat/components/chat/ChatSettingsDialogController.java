package net.zonia3000.ombrachat.components.chat;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.GpgUtils;
import net.zonia3000.ombrachat.Settings;
import org.drinkless.tdlib.TdApi;

public class ChatSettingsDialogController {

    @FXML
    private ComboBox<GpgUtils.GpgPublicKey> keysComboBox;
    @FXML
    private CheckBox enableGPGCheckBox;
    @FXML
    private Button saveBtn;

    private Settings settings;
    private TdApi.Chat selectedChat;
    private Runnable onClose;

    public void init(Settings settings, TdApi.Chat selectedChat, Runnable onClose) {
        this.settings = settings;
        this.selectedChat = selectedChat;
        this.onClose = onClose;

        var keys = GpgUtils.listKeys();
        for (var k : keys) {
            keysComboBox.getItems().add(k);
        }
        String chatKeyFingerprint = settings.getChatKey(selectedChat.id);
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
        onClose.run();
    }
}
