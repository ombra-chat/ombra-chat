package net.zonia3000.ombrachat.components;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.Mediator;
import net.zonia3000.ombrachat.components.chat.ChatFolderItem;

public class SettingsDialogController {

    @FXML
    private ComboBox chatFolderComboBox;

    private Mediator mediator;

    public void setMediator(Mediator mediator) {
        this.mediator = mediator;
        setChatFolders();
    }

    private void setChatFolders() {
        chatFolderComboBox.getItems().add(new ChatFolderItem(0, "All"));
        chatFolderComboBox.setValue("All");
        for (var chatFolderInfo : mediator.getChatFolderInfos()) {
            chatFolderComboBox.getItems().add(new ChatFolderItem(chatFolderInfo.id, chatFolderInfo.name.text.text));
        }
        setSelectedValue();
    }

    private void setSelectedValue() {
        int selectedFolderId = mediator.getSettings().getDefaultFolder();
        for (var item : chatFolderComboBox.getItems()) {
            ChatFolderItem cfi = (ChatFolderItem) item;
            if (cfi.getId() == selectedFolderId) {
                chatFolderComboBox.setValue(cfi.getLabel());
                return;
            }
        }
    }

    @FXML
    private void saveSettings() {
        ChatFolderItem selectedFolder = (ChatFolderItem) chatFolderComboBox.getValue();
        mediator.getSettings().setDefaultFolder(selectedFolder.getId());
        Stage stage = (Stage) chatFolderComboBox.getScene().getWindow();
        stage.close();
    }
}
