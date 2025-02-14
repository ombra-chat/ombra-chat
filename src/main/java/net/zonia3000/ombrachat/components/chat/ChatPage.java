package net.zonia3000.ombrachat.components.chat;

import java.io.IOError;
import java.io.IOException;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.ChatsLoader;
import net.zonia3000.ombrachat.MessagesLoader;
import net.zonia3000.ombrachat.Settings;
import net.zonia3000.ombrachat.components.chat.message.MessageNotSupportedBox;
import net.zonia3000.ombrachat.components.chat.message.MessagePhotoBox;
import net.zonia3000.ombrachat.components.chat.message.MessageTextBox;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

public class ChatPage extends VBox {

    @FXML
    private Label chatTitleLabel;
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private VBox chatContent;
    @FXML
    private Label gpgKeyLabel;

    private Client client;
    private ChatsLoader chatsLoader;
    private MessagesLoader messagesLoader;
    private Settings settings;

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

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setChatsLoader(ChatsLoader loader) {
        this.chatsLoader = loader;
    }

    public void setMessagesLoader(MessagesLoader loader) {
        this.messagesLoader = loader;
    }

    public void setSelectedChat(TdApi.Chat selectedChat) {
        this.selectedChat = selectedChat;
        setGpgKeyLabel();
        chatContent.getChildren().removeAll(chatContent.getChildren());
        chatTitleLabel.setText(selectedChat.title);
        scrollToBottom = true;
    }

    private void setGpgKeyLabel() {
        String chatKeyFingerprint = settings.getChatKey(selectedChat.id);
        gpgKeyLabel.managedProperty().bind(gpgKeyLabel.visibleProperty());
        if (chatKeyFingerprint == null) {
            gpgKeyLabel.setText("");
            gpgKeyLabel.setVisible(false);
        } else {
            gpgKeyLabel.setText(chatKeyFingerprint);
            gpgKeyLabel.setVisible(true);
        }
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
        } else if (content instanceof TdApi.MessagePhoto messagePhoto) {
            return new MessagePhotoBox(messagePhoto, client);
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
        } else {
            addSenderLabel(vbox, message.senderId);
        }
        return vbox;
    }

    private void addSenderLabel(VBox vbox, TdApi.MessageSender sender) {
        TdApi.Chat chat = getSenderChat(sender);
        if (chat == null) {
            return;
        }
        var label = new Label(chat.title);
        label.setTextFill(Color.BLUE);
        label.getStyleClass().add("bold");
        label.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            //messagesLoader.setSelectedChat(chat);
        });
        vbox.getChildren().add(label);
    }

    private TdApi.Chat getSenderChat(TdApi.MessageSender sender) {
        if (sender instanceof TdApi.MessageSenderChat senderChat) {
            return chatsLoader.getChat(senderChat.chatId);
        } else if (sender instanceof TdApi.MessageSenderUser senderUser) {
            return chatsLoader.getChat(senderUser.userId);
        }
        return null;
    }

    @FXML
    private void openChatSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader();

            loader.setLocation(ChatSettingsDialogController.class.getResource("/view/chat-settings-dialog.fxml"));
            Parent root = loader.load();
            ChatSettingsDialogController controller = loader.getController();

            Scene scene = new Scene(root);
            Stage newStage = new Stage();
            newStage.setTitle("Chat settings");
            newStage.setScene(scene);
            newStage.show();

            controller.init(settings, selectedChat, () -> {
                setGpgKeyLabel();
            });
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }
}
