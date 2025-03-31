package net.zonia3000.ombrachat;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.chat.ChatFolderItem;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.SettingsService;

public class SettingsDialogController {

    @FXML
    private ComboBox chatFolderComboBox;

    private SettingsService settings;
    private ChatsService chatsService;

    @FXML
    public void initialize() {
        this.settings = ServiceLocator.getService(SettingsService.class);
        this.chatsService = ServiceLocator.getService(ChatsService.class);
        setChatFolders();
    }

    private void setChatFolders() {
        chatFolderComboBox.getItems().add(new ChatFolderItem(0, "All"));
        chatFolderComboBox.setValue("All");
        for (var chatFolderInfo : chatsService.getChatFolderInfos()) {
            chatFolderComboBox.getItems().add(new ChatFolderItem(chatFolderInfo.id, chatFolderInfo.name.text.text));
        }
        setSelectedValue();
    }

    private void setSelectedValue() {
        int selectedFolderId = settings.getDefaultFolder();
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
        settings.setDefaultFolder(selectedFolder.getId());
        Stage stage = (Stage) chatFolderComboBox.getScene().getWindow();
        stage.close();
    }
}
