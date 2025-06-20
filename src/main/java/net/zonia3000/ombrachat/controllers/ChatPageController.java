package net.zonia3000.ombrachat.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.chat.message.FileBox;
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
import net.zonia3000.ombrachat.services.ThumbnailService;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatPageController {

    private static final Logger logger = LoggerFactory.getLogger(ChatPageController.class);

    private static final KeyCombination CTRL_V = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);

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
    private HBox newMessagesBox;
    @FXML
    private HBox replyToBox;
    @FXML
    private Text replyToUserLabel;
    @FXML
    private VBox selectedFilesBox;
    @FXML
    private TextField messageText;
    @FXML
    private HBox sendMessageBox;
    @FXML
    private VBox chatPage;

    private boolean scrollToBottom = true;
    private boolean loadingPreviousMessages = false;
    private boolean loadingNewMessages = false;
    private final List<File> selectedFiles = new ArrayList<>();

    private final GuiService guiService;
    private final ChatsService chatsService;
    private final MessagesService messagesService;
    private final SettingsService settings;
    private final TelegramClientService clientService;
    private final GpgService gpgService;
    private final ThumbnailService thumbnailService;

    private final VBox container;

    private PGPPublicKey chatPublicKey;
    private TdApi.Message oldestUnreadMessage = null;
    private TdApi.Message replyToMessage;
    private String replyToQuote;

    public ChatPageController(VBox container) {
        this.guiService = ServiceLocator.getService(GuiService.class);
        this.chatsService = ServiceLocator.getService(ChatsService.class);
        this.messagesService = ServiceLocator.getService(MessagesService.class);
        this.settings = ServiceLocator.getService(SettingsService.class);
        this.clientService = ServiceLocator.getService(TelegramClientService.class);
        this.gpgService = ServiceLocator.getService(GpgService.class);
        this.thumbnailService = ServiceLocator.getService(ThumbnailService.class);

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

        // when new messages are added the content height changes
        chatContent.heightProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (scrollToBottom) {
                setScrollPosition(1.0);
            } else if (loadingPreviousMessages) {
                // keep the chat scroll to the last position, while older/newer messages are loaded
                setScrollPosition((newValue.doubleValue() - oldValue.doubleValue()) / newValue.doubleValue());
            }
            loadingNewMessages = false;
            loadingPreviousMessages = false;
        });

        chatScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            scrollToBottom = false;
            if (chatScrollPane.getVvalue() <= 0.0 && !loadingPreviousMessages && !loadingNewMessages) {
                // Top edge reached
                messagesService.loadPreviousMessages();
                loadingPreviousMessages = true;
            } else if (chatScrollPane.getVvalue() == 1 && !loadingPreviousMessages && !loadingNewMessages) {
                loadNewMessages();
            }
        });

        chatScrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            markVisibleMessagesAsRead();
        });

        guiService.registerController(ChatPageController.class, this);
        logger.debug("{} initialized", ChatPageController.class.getSimpleName());

        setupPasteHandler();
        UiUtils.setupMiddleButtonPasteHandler(messageText);

        chatPage.setOnDragOver(this::handleDragOver);
        chatPage.setOnDragExited(this::handleDragExited);
        chatPage.setOnDragDropped(this::handleDragDropped);

        UiUtils.setVisible(replyToBox, false);
    }

    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            if (!chatPage.getStyleClass().contains("dragging")) {
                chatPage.getStyleClass().add("dragging");
            }
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void handleDragExited(DragEvent event) {
        chatPage.getStyleClass().removeAll("dragging");
    }

    private void handleDragDropped(DragEvent event) {
        List<File> files = event.getDragboard().getFiles();
        if (!files.isEmpty()) {
            logger.debug("Dropped {} files", files.size());
            attachFiles(files);
        }
        event.setDropCompleted(true);
        event.consume();
    }

    private void setupPasteHandler() {
        messageText.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (CTRL_V.match(event)) {
                var clipboard = Clipboard.getSystemClipboard();
                if (clipboard.hasFiles()) {
                    logger.debug("Files pasted from clipboard");
                    attachFiles(clipboard.getFiles());
                    event.consume();
                }
            }
        });
    }

    @FXML
    private void loadNewMessages() {
        scrollToBottom = false;
        var lastLoadedMessage = getLastLoadedMessage();
        var selectedChat = chatsService.getSelectedChat();
        if (lastLoadedMessage != null && selectedChat != null && selectedChat.unreadCount > 0) {
            messagesService.loadNewMessages(lastLoadedMessage.id);
            loadingNewMessages = true;
        }
    }

    private void setScrollPosition(double vvalue) {
        if (vvalue < 0 || vvalue > 1) {
            logger.debug("Ignoring invalid vvalue {}", vvalue);
            return;
        }
        logger.debug("Setting vvalue to {}", vvalue);
        chatScrollPane.setVvalue(vvalue);
    }

    private void markVisibleMessagesAsRead() {

        var viewport = chatScrollPane.getViewportBounds();
        double viewportHeight = viewport.getMaxY() - viewport.getMinY();
        double totalHeight = chatScrollPane.getContent().getBoundsInLocal().getHeight();
        double maxY = chatScrollPane.vvalueProperty().doubleValue() * (totalHeight - viewportHeight) + viewportHeight;
        double minY = maxY - viewportHeight;

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
        clientService.sendClientMessage(new TdApi.ViewMessages(selectedChat.id, ids, null, false), (e) -> {
            if (e instanceof TdApi.Ok) {
                Platform.runLater(() -> {
                    for (Node node : chatContent.getChildrenUnmodifiable()) {
                        if (node instanceof MessageBubble bubble) {
                            if (Arrays.stream(ids).anyMatch(i -> bubble.getMessage().id == i)) {
                                bubble.setRead(true);
                            }
                        }
                    }
                    logger.debug("Marked {} messages as read", ids.length);
                });
            } else {
                logger.warn("Unexpected response while marking messages as read");
            }
        });
    }

    private TdApi.Message getLastLoadedMessage() {
        var nodes = chatContent.getChildrenUnmodifiable();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            var node = nodes.get(i);
            if (node instanceof MessageBubble bubble) {
                return bubble.getMessage();
            }
        }
        return null;
    }

    public void updateChat(TdApi.Chat chat) {
        var selectedChat = chatsService.getSelectedChat();
        if (selectedChat != null && chat.id == selectedChat.id) {
            UiUtils.setVisible(newMessagesBox, chat.unreadCount > 0);
        }
    }

    public void closeChat() {
        guiService.setSelectedChat(null);
        thumbnailService.cleanup();
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
        setReplyToMessage(null, null);
    }

    public void setReplyToMessage(TdApi.Message replyToMessage, String replyToQuote) {
        this.replyToMessage = replyToMessage;
        this.replyToQuote = replyToQuote;
        if (replyToMessage == null) {
            UiUtils.setVisible(replyToBox, false);
        } else {
            var senderTitle = messagesService.getSenderTitle(replyToMessage.senderId);
            replyToUserLabel.setText(senderTitle);
            UiUtils.setVisible(replyToBox, true);
        }
    }

    @FXML
    private void removeReplyTo() {
        setReplyToMessage(null, null);
    }

    public void updateMessageReactions(long messageId, TdApi.MessageReaction[] reactions) {
        var children = chatContent.getChildrenUnmodifiable();
        for (var child : children) {
            var bubble = (MessageBubble) child;
            if (bubble.getMessage().id == messageId) {
                bubble.setReactions(reactions);
                return;
            }
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

    public void addMessages(List<TdApi.Message> messages) {
        var children = chatContent.getChildren();
        synchronized (children) {
            var alreadyLoadedIds = children.stream()
                    .map(n -> (MessageBubble) n)
                    .map(n -> n.getMessage().id)
                    .collect(Collectors.toList());

            List<MessageBubble> bubblesToAdd = messages.stream()
                    // prevent adding the same message twice
                    .filter(m -> !alreadyLoadedIds.contains(m.id))
                    .sorted((m1, m2) -> m1.id < m2.id ? -1 : 1)
                    .map(m -> getMessageBubble(m))
                    .collect(Collectors.toList());

            if (bubblesToAdd.isEmpty()) {
                logger.debug("No bubbles to add");
                loadingNewMessages = false;
                loadingPreviousMessages = false;
                return;
            }

            for (var bubble : bubblesToAdd) {
                var message = bubble.getMessage();
                if (!bubble.isRead() && (oldestUnreadMessage == null || oldestUnreadMessage.id > message.id)) {
                    oldestUnreadMessage = message;
                }
            }

            if (alreadyLoadedIds.isEmpty() || bubblesToAdd.getFirst().getMessage().id > alreadyLoadedIds.getLast()) {
                logger.debug("Adding {} messages to end of chat page", bubblesToAdd.size());
                children.addAll(bubblesToAdd);
                var selectedChat = chatsService.getSelectedChat();
                var hasNewMessagesToLoad = selectedChat != null && selectedChat.unreadCount > 0;
                UiUtils.setVisible(newMessagesBox, hasNewMessagesToLoad);
            } else if (bubblesToAdd.getLast().getMessage().id < alreadyLoadedIds.getFirst()) {
                logger.debug("Adding {} messages to beginning of chat page", bubblesToAdd.size());
                children.addAll(0, bubblesToAdd);
            } else {
                var insertAt = 0;
                var firstIdToAdd = bubblesToAdd.getFirst().getMessage().id;
                for (var id : alreadyLoadedIds) {
                    if (id > firstIdToAdd) {
                        children.addAll(insertAt, bubblesToAdd);
                        break;
                    }
                    insertAt++;
                }
            }
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
                if (msgBox instanceof FileBox docBox) {
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
                } else if (bubble.isForwaredFrom(user)) {
                    bubble.setForwardedFrom(user.usernames.editableUsername);
                }
            }
        }
    }

    @FXML
    private void openFileDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Attach file");
        var files = fileChooser.showOpenMultipleDialog(messageText.getScene().getWindow());
        attachFiles(files);
    }

    private void attachFiles(List<File> files) {
        if (files == null) {
            return;
        }
        selectedFiles.addAll(files);
        for (var file : files) {
            selectedFilesBox.getChildren().add(getSelectedFileBox(file));
        }
    }

    private HBox getSelectedFileBox(File file) {
        HBox box = new HBox();
        box.setMaxWidth(10000);
        HBox.setHgrow(box, Priority.ALWAYS);

        Label selectedFileLabel = new Label(file.getName());
        selectedFileLabel.setMaxWidth(10000);
        HBox.setHgrow(selectedFileLabel, Priority.ALWAYS);
        box.getChildren().add(selectedFileLabel);

        // detect if selected file is an image
        try (InputStream in = new FileInputStream(file)) {
            var image = new Image(in);
            if (!image.isError()) {
                CheckBox checkbox = new CheckBox();
                checkbox.setText("image");
                checkbox.setSelected(true);
                box.getChildren().add(checkbox);
            }
        } catch (Exception ignore) {
        }

        Button removeSelectedFileBtn = new Button();
        removeSelectedFileBtn.getStyleClass().addAll("removeSelectedFileBtn", "btn", "btn-20");
        removeSelectedFileBtn.setOnAction((e) -> {
            selectedFiles.remove(file);
            selectedFilesBox.getChildren().remove(box);
        });
        box.getChildren().add(removeSelectedFileBtn);

        return box;
    }

    @FXML
    private void sendMessage() {
        var contents = getInputMessageContents();
        if (contents == null || contents.isEmpty()) {
            return;
        }
        TdApi.InputMessageReplyToMessage replyTo = null;
        if (replyToMessage != null) {
            TdApi.InputTextQuote textQuote = null;
            if (replyToQuote != null) {
                var formattedQuote = new TdApi.FormattedText(replyToQuote, null);
                textQuote = new TdApi.InputTextQuote(formattedQuote, 0);
            }
            replyTo = new TdApi.InputMessageReplyToMessage(
                    replyToMessage.id,
                    textQuote
            );
        }
        for (var content : contents) {
            clientService.sendClientMessage(new TdApi.SendMessage(chatsService.getSelectedChat().id, 0, replyTo, null, null, content), (TdApi.Object object) -> {
                if (object instanceof TdApi.Message message) {
                    Platform.runLater(() -> {
                        scrollToBottom = true;
                    });
                }
                if (content instanceof TdApi.InputMessagePhoto inputMessagePhoto) {
                    Platform.runLater(() -> {
                        thumbnailService.removeThumbnail(inputMessagePhoto);
                    });
                }
            });
        }
        selectedFiles.clear();
        selectedFilesBox.getChildren().clear();
        messageText.setText("");
        setReplyToMessage(null, null);
    }

    public void updateMessage(long oldMessageId, TdApi.Message updatedMessage) {
        var currentChat = chatsService.getSelectedChat();
        if (currentChat == null) {
            return;
        }
        if (currentChat.id != updatedMessage.chatId) {
            return;
        }

        var children = chatContent.getChildrenUnmodifiable();
        for (var child : children) {
            var bubble = (MessageBubble) child;
            if (bubble.getMessage().id == oldMessageId) {
                logger.debug("Message id changed: {} -> {}", oldMessageId, updatedMessage.id);
                bubble.updateMessage(updatedMessage);
            }
        }
    }

    private List<TdApi.InputMessageContent> getInputMessageContents() {
        List<TdApi.InputMessageContent> contents = new ArrayList<>();
        if (chatPublicKey == null) {
            var formattedText = new TdApi.FormattedText(messageText.getText(), new TdApi.TextEntity[]{});
            if (selectedFiles.isEmpty()) {
                contents.add(new TdApi.InputMessageText(formattedText, null, true));
            } else {
                int i = 0;
                for (var selectedFile : selectedFiles) {
                    var absolutePath = selectedFile.getAbsolutePath();
                    if (handleAttachedFileAsImage(i)) {
                        var messagePhoto = getMessagePhoto(absolutePath);
                        if (formattedText != null) {
                            messagePhoto.caption = formattedText;
                        }
                        contents.add(messagePhoto);
                    } else {
                        var inputFileLocal = new TdApi.InputFileLocal(absolutePath);
                        contents.add(new TdApi.InputMessageDocument(inputFileLocal, null, false, formattedText));
                    }
                    formattedText = null; // set formatted text only on first file
                    i++;
                }
            }
        } else {
            for (var selectedFile : selectedFiles) {
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

    private boolean handleAttachedFileAsImage(int index) {
        if (index >= selectedFilesBox.getChildren().size()) {
            return false;
        }
        HBox box = (HBox) selectedFilesBox.getChildren().get(index);
        if (box.getChildren().size() >= 2 && box.getChildren().get(1) instanceof CheckBox imageCheckbox) {
            return imageCheckbox.isSelected();
        }
        return false;
    }

    private TdApi.InputMessagePhoto getMessagePhoto(String imagePath) {
        var messagePhoto = new TdApi.InputMessagePhoto();
        messagePhoto.photo = new TdApi.InputFileLocal(imagePath);
        messagePhoto.thumbnail = thumbnailService.createThumbnail(imagePath);
        return messagePhoto;
    }

    private MessageBubble getMessageBubble(TdApi.Message message) {
        MessageBubble bubble = new MessageBubble(message);
        bubble.setMessageContent(getMessageContentBox(message.content));
        bubble.setRead(bubble.isMy() || isRead(message));
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

        if (nodesToRemove.isEmpty()) {
            logger.warn("No messages to remove found. Ids to remove: {}", messageIds);
            return;
        }

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
            UiUtils.setAppIcon(newStage);
            newStage.setTitle("Chat settings");
            newStage.setScene(scene);
            newStage.show();
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }
}
