package net.zonia3000.ombrachat.controllers;

import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.chat.ChatSelectionView;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.GuiService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardDialogController {

    private static final Logger logger = LoggerFactory.getLogger(ForwardDialogController.class);

    @FXML
    private TextField searchField;
    @FXML
    private ChatSelectionView chatSelectionView;
    @FXML
    private CheckBox sendCopy;

    private final ChatsService chatsService;
    private final GuiService guiService;
    private final TelegramClientService clientService;

    private TdApi.Message messageToForward;

    public ForwardDialogController() {
        chatsService = ServiceLocator.getService(ChatsService.class);
        guiService = ServiceLocator.getService(GuiService.class);
        clientService = ServiceLocator.getService(TelegramClientService.class);
    }

    public void setMessageToForward(TdApi.Message messageToForward) {
        this.messageToForward = messageToForward;
    }

    @FXML
    public void initialize() {
        initSearchField();
        initChatsList();
        logger.debug("initialized");
    }

    private void initSearchField() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                logger.debug("Searching {}", newValue);
                var chats = chatsService.getChatStream()
                        .filter(c -> c.permissions.canSendBasicMessages)
                        .filter(c -> c.title.toLowerCase().contains(newValue.toLowerCase()))
                        .collect(Collectors.toList());
                chatSelectionView.setChatsList(chats);
            }
        });
    }

    private void initChatsList() {
        var chats = chatsService.getChatStream()
                .filter(c -> c.permissions.canSendBasicMessages)
                .collect(Collectors.toList());
        chatSelectionView.setChatsList(chats);

        chatSelectionView.setAction(chat -> {
            logger.debug("Forwarding message");
            clientService.sendClientMessage(new TdApi.ForwardMessages(
                    chat.id,
                    0,
                    messageToForward.chatId,
                    new long[]{messageToForward.id},
                    null,
                    sendCopy.isSelected(),
                    false
            ), (r) -> {
                Platform.runLater(() -> {
                    guiService.setSelectedChat(chat);
                });
            });
            closeDialog();
        });
    }

    private void closeDialog() {
        Stage stage = (Stage) chatSelectionView.getScene().getWindow();
        stage.close();
    }
}
