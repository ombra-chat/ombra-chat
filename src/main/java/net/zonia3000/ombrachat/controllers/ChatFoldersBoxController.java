package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.chat.ChatFolderItem;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.GuiService;
import net.zonia3000.ombrachat.services.SettingsService;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatFoldersBoxController extends HBox {

    private static final Logger logger = LoggerFactory.getLogger(ChatFoldersBoxController.class);

    @FXML
    private ComboBox chatFolderComboBox;

    private final ChatsService chatsService;
    private final GuiService guiService;

    public ChatFoldersBoxController() {
        chatsService = ServiceLocator.getService(ChatsService.class);
        guiService = ServiceLocator.getService(GuiService.class);
    }

    @FXML
    public void initialize() {
        var info = chatsService.getChatFolderInfos();
        setChatFolders(info);
    }

    public void setChatFolders(TdApi.ChatFolderInfo[] chatFolderInfos) {
        chatFolderComboBox.getItems().add(new ChatFolderItem(0, "All"));
        chatFolderComboBox.setValue("All");
        for (var chatFolderInfo : chatFolderInfos) {
            chatFolderComboBox.getItems().add(new ChatFolderItem(chatFolderInfo.id, chatFolderInfo.name.text.text));
        }

        setSelectedValue();

        chatFolderComboBox.setOnAction(event -> {
            ChatFolderItem selectedItem = (ChatFolderItem) chatFolderComboBox.getValue();
            chatsService.setSelectedFolder(selectedItem.getId());
            // hide current chat
            guiService.setSelectedChat(null);
        });
    }

    private void setSelectedValue() {
        var settings = ServiceLocator.getService(SettingsService.class);
        int selectedFolderId = settings.getDefaultFolder();
        for (var item : chatFolderComboBox.getItems()) {
            ChatFolderItem cfi = (ChatFolderItem) item;
            if (cfi.getId() == selectedFolderId) {
                if (cfi.getId() != 0) {
                    chatsService.setSelectedFolder(cfi.getId());
                }
                chatFolderComboBox.setValue(cfi.getLabel());
                return;
            }
        }
    }
}
