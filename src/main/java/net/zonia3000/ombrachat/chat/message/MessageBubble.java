package net.zonia3000.ombrachat.chat.message;

import java.io.IOError;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.controllers.MessageDialogController;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.UserService;
import org.drinkless.tdlib.TdApi;

public class MessageBubble extends VBox {

    private final ChatsService chatsService;
    private final UserService userService;
    private final TdApi.Message message;

    private final Label senderLabel = new Label();
    private final Button actionsButton = new Button();

    private boolean read;
    private boolean processingRead;
    private boolean my;
    private boolean gpg;

    public MessageBubble(TdApi.Message message) {
        this.chatsService = ServiceLocator.getService(ChatsService.class);
        this.userService = ServiceLocator.getService(UserService.class);
        this.message = message;
        getStyleClass().add("message-bubble");
        setMy(message.senderId instanceof TdApi.MessageSenderUser senderUser && senderUser.userId == userService.getMyId());
        initHeader();
    }

    private void initHeader() {
        HBox headerBox = new HBox();
        initSenderLabel();
        headerBox.getChildren().add(senderLabel);
        actionsButton.getStyleClass().addAll("btn", "btn-20", "message-action-btn");
        actionsButton.setOnAction((e) -> openMessageDialog());
        headerBox.getChildren().add(actionsButton);
        getChildren().add(headerBox);
    }

    private void initSenderLabel() {
        senderLabel.setTextFill(Color.BLUE);
        senderLabel.getStyleClass().add("bold");
        senderLabel.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            // TODO
        });
        senderLabel.setMaxWidth(10000);
        HBox.setHgrow(senderLabel, Priority.ALWAYS);

        if (!my) {
            setSender();
        }
    }

    public TdApi.Message getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isProcessingRead() {
        return processingRead;
    }

    public void setProcessingRead(boolean processingRead) {
        this.processingRead = processingRead;
    }

    public boolean isMy() {
        return my;
    }

    private void setMy(boolean my) {
        this.my = my;
        if (my) {
            getStyleClass().add("my-message");
        }
    }

    public boolean isGpg() {
        return gpg;
    }

    private void setGpg(boolean gpg) {
        this.gpg = gpg;
        if (gpg) {
            getStyleClass().add("gpg-message");
        }
    }

    public void setMessageContent(VBox content) {
        setGpg(content instanceof MessageGpgTextBox || content instanceof MessageGpgDocumentBox);
        getChildren().add(content);
        setFooter();
    }

    private void setFooter() {
        HBox footer = new HBox();
        Label dateLabel = new Label();
        dateLabel.setText(formatDate());
        dateLabel.setMaxWidth(10000);
        dateLabel.getStyleClass().add("msg-date");
        HBox.setHgrow(dateLabel, Priority.ALWAYS);
        footer.getChildren().add(dateLabel);
        getChildren().add(footer);
    }

    private String formatDate() {
        Date currentDate = new Date(1000l * message.date);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return formatter.format(currentDate);
    }

    private void setSender() {
        var id = getSenderChatId();
        if (id == null) {
            return;
        }
        TdApi.Chat chat = chatsService.getChat(id);
        if (chat == null) {
            // TODO
        } else {
            senderLabel.setText(chat.title);
        }
    }

    private Long getSenderChatId() {
        var sender = message.senderId;
        if (sender instanceof TdApi.MessageSenderChat senderChat) {
            return senderChat.chatId;
        } else if (sender instanceof TdApi.MessageSenderUser senderUser) {
            return senderUser.userId;
        }
        return null;
    }

    private void openMessageDialog() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MessageDialogController.class.getResource("/view/message-dialog.fxml"));
            Parent root = loader.load();
            ((MessageDialogController) loader.getController()).setMessage(message);
            Scene scene = new Scene(root);
            UiUtils.setCommonCss(scene);
            Stage newStage = new Stage();
            newStage.setTitle("Message");
            newStage.setScene(scene);
            newStage.showAndWait();
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }
}
