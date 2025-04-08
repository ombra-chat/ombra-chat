package net.zonia3000.ombrachat.chat.message;

import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.services.ChatsService;
import org.drinkless.tdlib.TdApi;

public class MessageBubble extends VBox {
    
    private final ChatsService chatsService;
    private final TdApi.Message message;

    private boolean read;
    private boolean processingRead;
    private boolean my;
    private boolean gpg;

    public MessageBubble(TdApi.Message message) {
        this.chatsService = ServiceLocator.getService(ChatsService.class);
        this.message = message;
        getStyleClass().add("message-bubble");
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

    public void setMy(boolean my) {
        this.my = my;
        if (my) {
            getStyleClass().add("my-message");
        }
    }

    public boolean isGpg() {
        return gpg;
    }

    public void setGpg(boolean gpg) {
        this.gpg = gpg;
        if (gpg) {
            getStyleClass().add("gpg-message");
        }
    }

    public void setSender(TdApi.MessageSender sender) {
        TdApi.Chat chat = getSenderChat(sender);
        if (chat == null) {
            return;
        }
        var label = new Label(chat.title);
        label.setTextFill(Color.BLUE);
        label.getStyleClass().add("bold");
        label.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            // TODO
        });
        getChildren().add(label);
    }

    private TdApi.Chat getSenderChat(TdApi.MessageSender sender) {
        if (sender instanceof TdApi.MessageSenderChat senderChat) {
            return chatsService.getChat(senderChat.chatId);
        } else if (sender instanceof TdApi.MessageSenderUser senderUser) {
            return chatsService.getChat(senderUser.userId);
        }
        return null;
    }
}
