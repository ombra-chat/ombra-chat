package net.zonia3000.ombrachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import org.drinkless.tdlib.TdApi;

public class MessageDialogController {

    private TdApi.Message message;
    private TdApi.Chat chat;

    @FXML
    private Button deleteMessageBtn;

    public void setMessage(TdApi.Message message) {
        this.message = message;
        chat = ServiceLocator.getService(ChatsService.class).getSelectedChat();
        if (!chat.canBeDeletedForAllUsers && !chat.canBeDeletedOnlyForSelf) {
            this.deleteMessageBtn.setDisable(true);
        }
    }

    @FXML
    private void deleteMessage() {
        var client = ServiceLocator.getService(TelegramClientService.class);
        client.sendClientMessage(new TdApi.DeleteMessages(chat.id, new long[]{message.id}, !chat.canBeDeletedOnlyForSelf));
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) deleteMessageBtn.getScene().getWindow();
        stage.close();
    }
}
