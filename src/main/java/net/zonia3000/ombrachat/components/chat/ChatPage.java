package net.zonia3000.ombrachat.components.chat;

import java.io.IOError;
import java.io.IOException;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import net.zonia3000.ombrachat.MessagesLoader;
import net.zonia3000.ombrachat.components.chat.message.MessageNotSupportedBox;
import net.zonia3000.ombrachat.components.chat.message.MessageTextBox;
import org.drinkless.tdlib.TdApi;

public class ChatPage extends VBox {

    @FXML
    private Label chatTitleLabel;
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private VBox chatContent;

    private MessagesLoader messagesLoader;

    private TdApi.Chat selectedChat;
    private long myId;
    private boolean scrollToBottom = true;

    private boolean loading = false;

    public ChatPage() {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatFoldersBox.class.getResource("/view/chat-page.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            throw new IOError(ex);
        }
        chatScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        chatScrollPane.setFitToWidth(true);
        chatContent.setSpacing(10);

        chatContent.heightProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (scrollToBottom) {
                chatScrollPane.setVvalue(1.0);
            } else {
                chatScrollPane.setVvalue((newValue.doubleValue() - oldValue.doubleValue()) / newValue.doubleValue());
                loading = false;
            }
        });

        chatScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (chatScrollPane.getVvalue() <= 0.0 && !loading) {
                // Top edge reached
                scrollToBottom = false;
                messagesLoader.loadPreviousMessages();
                loading = true;
            }
        });
    }

    public void setMessagesLoader(MessagesLoader loader) {
        this.messagesLoader = loader;
    }

    public void setSelectedChat(TdApi.Chat selectedChat) {
        chatContent.getChildren().removeAll(chatContent.getChildren());
        chatTitleLabel.setText(selectedChat.title);
        scrollToBottom = true;
    }

    public void setMyId(long myId) {
        this.myId = myId;
    }

    public void addMessage(TdApi.Message message) {
        var bubble = getMessageBubble(message);
        bubble.getChildren().add(getMessageContentBox(message.content));
        chatContent.getChildren().addFirst(bubble);
    }

    private VBox getMessageContentBox(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText messageText) {
            return new MessageTextBox(messageText);
        } else {
            return new MessageNotSupportedBox(content);
        }
    }

    @FXML
    private void handleSendMessageClick() {
    }

    private VBox getMessageBubble(TdApi.Message message) {
        VBox vbox = new VBox();
        vbox.getStyleClass().add("message-bubble");
        if (message.senderId instanceof TdApi.MessageSenderUser senderUser && senderUser.userId == myId) {
            vbox.getStyleClass().add("my-message");
        }
        return vbox;
    }
}
