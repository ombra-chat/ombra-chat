package net.zonia3000.ombrachat.chat;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.Mediator;
import net.zonia3000.ombrachat.chat.message.MessageNotSupportedBox;
import net.zonia3000.ombrachat.chat.message.MessagePhotoBox;
import net.zonia3000.ombrachat.chat.message.MessageTextBox;
import net.zonia3000.ombrachat.events.ChatSelected;
import net.zonia3000.ombrachat.events.ChatSettingsSaved;
import net.zonia3000.ombrachat.events.LoadPreviousMessages;
import net.zonia3000.ombrachat.events.MessageReceived;
import net.zonia3000.ombrachat.events.SendClientMessage;
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
    @FXML
    private Label selectedFileLabel;
    @FXML
    private TextField messageText;
    @FXML
    private Button removeSelectedFileBtn;

    private Mediator mediator;

    private boolean scrollToBottom = true;
    private boolean loading = false;
    private File selectedFile = null;

    public ChatPage() {
        setVisible(false);

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
                mediator.publish(new LoadPreviousMessages());
                loading = true;
            }
        });

        selectedFileLabel.managedProperty().bind(selectedFileLabel.visibleProperty());
        removeSelectedFileBtn.managedProperty().bind(removeSelectedFileBtn.visibleProperty());
        selectedFileLabel.setVisible(false);
        removeSelectedFileBtn.setVisible(false);
    }

    public void setMediator(Mediator mediator) {
        this.mediator = mediator;
        mediator.subscribe(ChatSelected.class, (e) -> setSelectedChat(e.getChat()));
        mediator.subscribe(MessageReceived.class, (e) -> {
            Platform.runLater(() -> {
                prependMessage(e.getMessage());
            });
        });
        mediator.subscribe(ChatSettingsSaved.class, (e) -> setGpgKeyLabel());
    }

    private void setSelectedChat(TdApi.Chat selectedChat) {
        if (selectedChat == null) {
            setVisible(false);
        } else {
            setGpgKeyLabel();
            chatContent.getChildren().removeAll(chatContent.getChildren());
            chatTitleLabel.setText(selectedChat.title);
            scrollToBottom = true;
            setVisible(true);
        }
    }

    private void setGpgKeyLabel() {
        String chatKeyFingerprint = mediator.getChatKeyFingerprint();
        gpgKeyLabel.managedProperty().bind(gpgKeyLabel.visibleProperty());
        if (chatKeyFingerprint == null) {
            gpgKeyLabel.setText("");
            gpgKeyLabel.setVisible(false);
        } else {
            gpgKeyLabel.setText(chatKeyFingerprint);
            gpgKeyLabel.setVisible(true);
        }
    }

    private void prependMessage(TdApi.Message message) {
        chatContent.getChildren().addFirst(getMessageBubble(message));
    }

    private void appendMessage(TdApi.Message message) {
        chatContent.getChildren().add(getMessageBubble(message));
    }

    private VBox getMessageContentBox(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText messageText) {
            return new MessageTextBox(messageText);
        } else if (content instanceof TdApi.MessagePhoto messagePhoto) {
            return new MessagePhotoBox(mediator, messagePhoto);
        } else {
            return new MessageNotSupportedBox(content);
        }
    }

    private void setSelectedFileLabel() {
        if (selectedFile == null) {
            selectedFileLabel.setText("");
            selectedFileLabel.setVisible(false);
            removeSelectedFileBtn.setVisible(false);
        } else {
            selectedFileLabel.setText(selectedFile.getName());
            selectedFileLabel.setVisible(true);
            removeSelectedFileBtn.setVisible(true);
        }
    }

    @FXML
    private void openFileDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Attach file");
        selectedFile = fileChooser.showOpenDialog(messageText.getScene().getWindow());
        setSelectedFileLabel();
    }

    @FXML
    private void removeSelectedFile() {
        selectedFile = null;
        setSelectedFileLabel();
    }

    @FXML
    private void sendMessage() {
        mediator.publish(new SendClientMessage(new TdApi.SendMessage(mediator.getSelectedChat().id, 0, null, null, null, getInputMessageContent()), (TdApi.Object object) -> {
            if (object instanceof TdApi.Message message) {
                Platform.runLater(() -> {
                    scrollToBottom = true;
                    appendMessage(message);
                    removeSelectedFile();
                });
            }
        }));
        messageText.setText("");
    }

    private TdApi.InputMessageContent getInputMessageContent() {
        var formattedText = new TdApi.FormattedText(messageText.getText(), new TdApi.TextEntity[]{});
        if (selectedFile == null) {
            return new TdApi.InputMessageText(formattedText, null, true);
        } else {
            var inputFileLocal = new TdApi.InputFileLocal(selectedFile.getAbsolutePath());
            return new TdApi.InputMessageDocument(inputFileLocal, null, false, formattedText);
        }
    }

    private VBox getMessageBubble(TdApi.Message message) {
        VBox bubble = new VBox();
        bubble.getStyleClass().add("message-bubble");
        if (message.senderId instanceof TdApi.MessageSenderUser senderUser && senderUser.userId == mediator.getMyId()) {
            bubble.getStyleClass().add("my-message");
        } else {
            addSenderLabel(bubble, message.senderId);
        }
        bubble.getChildren().add(getMessageContentBox(message.content));
        return bubble;
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
            return mediator.getChat(senderChat.chatId);
        } else if (sender instanceof TdApi.MessageSenderUser senderUser) {
            return mediator.getChat(senderUser.userId);
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
            controller.setMediator(mediator);

            Scene scene = new Scene(root);
            Stage newStage = new Stage();
            newStage.setTitle("Chat settings");
            newStage.setScene(scene);
            newStage.show();
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }
}
