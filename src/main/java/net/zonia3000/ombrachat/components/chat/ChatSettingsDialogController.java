package net.zonia3000.ombrachat.components.chat;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.GpgUtils;
import net.zonia3000.ombrachat.Mediator;
import net.zonia3000.ombrachat.events.ChatSettingsSaved;

public class ChatSettingsDialogController {

    @FXML
    private ComboBox<GpgUtils.GpgPublicKey> keysComboBox;
    @FXML
    private CheckBox enableGPGCheckBox;
    @FXML
    private Button saveBtn;

    private Mediator mediator;

    public void setMediator(Mediator mediator) {
        this.mediator = mediator;

        var keys = GpgUtils.listKeys();
        for (var k : keys) {
            keysComboBox.getItems().add(k);
        }
        String chatKeyFingerprint = mediator.getSettings().getChatKey(mediator.getSelectedChat().id);
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
            mediator.getSettings().setChatKey(mediator.getSelectedChat().id, selected.getFingerprint());
        } else {
            mediator.getSettings().setChatKey(mediator.getSelectedChat().id, null);
        }

        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
        mediator.publish(new ChatSettingsSaved());
    }
}
