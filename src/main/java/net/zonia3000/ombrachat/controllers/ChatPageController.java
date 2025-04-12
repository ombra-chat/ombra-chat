package net.zonia3000.ombrachat.controllers;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.chat.message.MessageBubble;
import net.zonia3000.ombrachat.chat.message.MessageDocumentBox;
import net.zonia3000.ombrachat.chat.message.MessageGpgDocumentBox;
import net.zonia3000.ombrachat.chat.message.MessageGpgTextBox;
import net.zonia3000.ombrachat.chat.message.MessageNotSupportedBox;
import net.zonia3000.ombrachat.chat.message.MessagePhotoBox;
import net.zonia3000.ombrachat.chat.message.MessageTextBox;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.GuiService;
import net.zonia3000.ombrachat.services.MessagesService;
import net.zonia3000.ombrachat.services.SettingsService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatPageController {

    private static final Logger logger = LoggerFactory.getLogger(ChatPageController.class);

    @FXML
    private ImageView lockImageView;
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
    @FXML
    private HBox sendMessageBox;

    private boolean scrollToBottom = true;
    private boolean loading = false;
    private File selectedFile = null;

    private final GuiService guiService;
    private final ChatsService chatsService;
    private final MessagesService messagesService;
    private final SettingsService settings;
    private final TelegramClientService clientService;
    private final GpgService gpgService;

    private final VBox container;

    private PGPPublicKey chatPublicKey;

    public ChatPageController(VBox container) {
        this.guiService = ServiceLocator.getService(GuiService.class);
        this.chatsService = ServiceLocator.getService(ChatsService.class);
        this.messagesService = ServiceLocator.getService(MessagesService.class);
        this.settings = ServiceLocator.getService(SettingsService.class);
        this.clientService = ServiceLocator.getService(TelegramClientService.class);
        this.gpgService = ServiceLocator.getService(GpgService.class);

        this.container = container;
    }

    private void setVisible(boolean visible) {
        container.setVisible(visible);
    }

    @FXML
    public void initialize() {
        container.setVisible(false);

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
                messagesService.loadPreviousMessages();
                loading = true;
            }
        });

        chatScrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            markVisibleMessagesAsRead();
        });

        selectedFileLabel.managedProperty().bind(selectedFileLabel.visibleProperty());
        removeSelectedFileBtn.managedProperty().bind(removeSelectedFileBtn.visibleProperty());
        selectedFileLabel.setVisible(false);
        removeSelectedFileBtn.setVisible(false);

        guiService.registerController(ChatPageController.class, this);
        logger.debug("{} initialized", ChatPageController.class.getSimpleName());
    }

    private void markVisibleMessagesAsRead() {

        var viewport = chatScrollPane.getViewportBounds();
        double availableHeight = viewport.getMaxY() - viewport.getMinY();
        double totalHeight = chatScrollPane.getContent().getBoundsInLocal().getHeight();
        double maxY = chatScrollPane.vvalueProperty().doubleValue() * (totalHeight - availableHeight) + availableHeight;
        double minY = maxY - availableHeight;

        VBox content = (VBox) chatScrollPane.getContent();

        List<Long> messagesToBeMarkedAsRead = new ArrayList<>();
        for (Node node : content.getChildren()) {
            if (node instanceof MessageBubble bubble) {
                var bubbleBounds = bubble.getBoundsInParent();
                var bubbleMinY = bubbleBounds.getMinY();
                var bubbleMaxY = bubbleBounds.getMaxY();
                if (bubbleMinY < 0) {
                    // in local bounds, the minimum position is 0;
                    // negative values appear in uninitialized items
                    return;
                }
                // detect visible and unread messages
                if (((bubbleMinY >= minY && bubbleMaxY <= maxY)
                        // message higher than window
                        || (bubbleMinY <= minY && bubbleMaxY >= maxY))
                        && !bubble.isRead() && !bubble.isProcessingRead()) {
                    bubble.setProcessingRead(true);
                    messagesToBeMarkedAsRead.add(bubble.getMessage().id);
                }
            }
        }
        markMessagesAsRead(messagesToBeMarkedAsRead);
    }

    private void markMessagesAsRead(List<Long> messagesToBeMarkedAsRead) {
        if (messagesToBeMarkedAsRead.isEmpty()) {
            return;
        }
        var selectedChat = chatsService.getSelectedChat();
        if (selectedChat == null) {
            return;
        }
        long[] ids = messagesToBeMarkedAsRead.stream().mapToLong(Long::longValue).toArray();
        clientService.sendClientMessage(new TdApi.ViewMessages(selectedChat.id, ids, null, false));
    }

    public void closeChat() {
        guiService.setSelectedChat(null);
    }

    public void setSelectedChat(TdApi.Chat selectedChat) {
        if (selectedChat == null) {
            setVisible(false);
        } else {
            setGpgKeyLabel();
            chatContent.getChildren().removeAll(chatContent.getChildren());
            lockImageView.setImage(guiService.getLockImage());
            UiUtils.setVisible(lockImageView, selectedChat.type instanceof TdApi.ChatTypeSecret);
            chatTitleLabel.setText(selectedChat.title);
            scrollToBottom = true;
            var writableChat = selectedChat.permissions.canSendBasicMessages;
            UiUtils.setVisible(sendMessageBox, writableChat);
            setVisible(true);
        }
    }

    public void setGpgKeyLabel() {
        String chatKeyFingerprint = settings.getChatKeyFingerprint(chatsService.getSelectedChat().id);
        gpgKeyLabel.managedProperty().bind(gpgKeyLabel.visibleProperty());
        if (chatKeyFingerprint == null) {
            gpgKeyLabel.setText("");
            gpgKeyLabel.setVisible(false);
            chatPublicKey = null;
        } else {
            gpgKeyLabel.setText(chatKeyFingerprint);
            gpgKeyLabel.setVisible(true);
            chatPublicKey = gpgService.getEncryptionKey(chatKeyFingerprint);
        }
    }

    public void addMessage(TdApi.Message message) {
        logger.debug("Adding message to chat page");
        var children = chatContent.getChildren();
        synchronized (children) {
            if (children.stream().map(n -> (MessageBubble) n).anyMatch(m -> m.getMessage().id == message.id)) {
                // prevent adding the same message twice
                return;
            }
            MessageBubble nextMessage = children.stream()
                    .map(n -> (MessageBubble) n)
                    .filter(m -> m.getMessage().id > message.id)
                    .findFirst().orElse(null);
            MessageBubble bubble = getMessageBubble(message);
            if (nextMessage != null) {
                var index = children.indexOf(nextMessage);
                if (index != -1) {
                    children.add(index, bubble);
                    return;
                }
            }
            children.add(bubble);
        }
        markVisibleMessagesAsRead();
    }

    private boolean isRead(TdApi.Message message) {
        var selectedChat = chatsService.getSelectedChat();
        if (selectedChat == null) {
            return false;
        }
        return message.id <= selectedChat.lastReadInboxMessageId;
    }

    private VBox getMessageContentBox(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText messageText) {
            return new MessageTextBox(messageText);
        } else if (content instanceof TdApi.MessagePhoto messagePhoto) {
            return new MessagePhotoBox(messagePhoto);
        } else if (content instanceof TdApi.MessageDocument messageDocument) {
            if (chatPublicKey != null && gpgService.isGpgMessage(messageDocument)) {
                if (gpgService.isGpgTextMessage(messageDocument)) {
                    return new MessageGpgTextBox(messageDocument);
                } else {
                    return new MessageGpgDocumentBox(messageDocument);
                }
            } else {
                return new MessageDocumentBox(messageDocument);
            }
        } else {
            return new MessageNotSupportedBox(content);
        }
    }

    public void handleFileUpdated(TdApi.UpdateFile update) {
        for (Node node : chatContent.getChildrenUnmodifiable()) {
            if (node instanceof MessageBubble bubble) {
                var msgBox = bubble.getContentBox();
                if (msgBox instanceof MessageDocumentBox docBox) {
                    if (docBox.updateFile(update)) {
                        return;
                    }
                }
            }
        }
    }

    public void updateUserOnMessages(TdApi.User user) {
        for (Node node : chatContent.getChildrenUnmodifiable()) {
            if (node instanceof MessageBubble bubble) {
                if (bubble.isFrom(user)) {
                    bubble.setSender(user.usernames.editableUsername);
                }
            }
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
        var contents = getInputMessageContents();
        if (contents == null || contents.isEmpty()) {
            return;
        }
        for (var content : contents) {
            clientService.sendClientMessage(new TdApi.SendMessage(chatsService.getSelectedChat().id, 0, null, null, null, content), (TdApi.Object object) -> {
                if (object instanceof TdApi.Message message) {
                    Platform.runLater(() -> {
                        scrollToBottom = true;
                    });
                }
            });
        }
        removeSelectedFile();
        messageText.setText("");
    }

    private List<TdApi.InputMessageContent> getInputMessageContents() {
        List<TdApi.InputMessageContent> contents = new ArrayList<>();
        if (chatPublicKey == null) {
            var formattedText = new TdApi.FormattedText(messageText.getText(), new TdApi.TextEntity[]{});
            if (selectedFile == null) {
                contents.add(new TdApi.InputMessageText(formattedText, null, true));
            } else {
                var inputFileLocal = new TdApi.InputFileLocal(selectedFile.getAbsolutePath());
                contents.add(new TdApi.InputMessageDocument(inputFileLocal, null, false, formattedText));
            }
        } else {
            if (selectedFile != null) {
                var file = gpgService.createGpgFile(chatPublicKey, selectedFile);
                if (file != null) {
                    var inputFileLocal = new TdApi.InputFileLocal(file.getAbsolutePath());
                    contents.add(new TdApi.InputMessageDocument(inputFileLocal, null, false, null));
                }
            }
            if (!messageText.getText().isBlank()) {
                var file = gpgService.createGpgTextFile(chatPublicKey, messageText.getText());
                if (file != null) {
                    var inputFileLocal = new TdApi.InputFileLocal(file.getAbsolutePath());
                    contents.add(new TdApi.InputMessageDocument(inputFileLocal, null, false, null));
                }
            }
        }
        return contents;
    }

    private MessageBubble getMessageBubble(TdApi.Message message) {
        MessageBubble bubble = new MessageBubble(message);
        bubble.setMessageContent(getMessageContentBox(message.content));
        bubble.setRead(isRead(message));
        return bubble;
    }

    public void deleteMessages(long chatId, long[] messageIds) {
        var currentChat = chatsService.getSelectedChat();
        if (currentChat == null) {
            return;
        }
        if (currentChat.id != chatId) {
            return;
        }
        var children = chatContent.getChildren();

        List<Node> nodesToRemove = children.stream()
                .map(n -> (MessageBubble) n)
                .filter(n -> Arrays.stream(messageIds).anyMatch(id -> n.getMessage().id == id))
                .collect(Collectors.toList());

        logger.debug("Removing {} messages from the list", nodesToRemove.size());
        children.removeAll(nodesToRemove);
    }

    @FXML
    private void openChatSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(ChatSettingsDialogController.class.getResource("/view/chat-settings-dialog.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            UiUtils.setCommonCss(scene);
            Stage newStage = new Stage();
            newStage.setTitle("Chat settings");
            newStage.setScene(scene);
            newStage.show();
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }
}
