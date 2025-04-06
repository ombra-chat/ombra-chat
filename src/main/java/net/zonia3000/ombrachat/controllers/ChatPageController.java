package net.zonia3000.ombrachat.controllers;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.chat.message.MessageDocumentBox;
import net.zonia3000.ombrachat.chat.message.MessageGpgDocumentBox;
import net.zonia3000.ombrachat.chat.message.MessageGpgTextBox;
import net.zonia3000.ombrachat.chat.message.MessageNotSupportedBox;
import net.zonia3000.ombrachat.chat.message.MessagePhotoBox;
import net.zonia3000.ombrachat.chat.message.MessageTextBox;
import net.zonia3000.ombrachat.events.ChatSelected;
import net.zonia3000.ombrachat.events.ChatSettingsSaved;
import net.zonia3000.ombrachat.events.FileUpdated;
import net.zonia3000.ombrachat.events.MessageReceived;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.GuiService;
import net.zonia3000.ombrachat.services.MessagesService;
import net.zonia3000.ombrachat.services.SettingsService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import net.zonia3000.ombrachat.services.UserService;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatPageController {

    private static final Logger logger = LoggerFactory.getLogger(ChatPageController.class);

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
    private final UserService userService;
    private final SettingsService settings;
    private final TelegramClientService clientService;
    private final GpgService gpgService;

    private final VBox container;

    private PGPPublicKey chatPublicKey;

    public ChatPageController(VBox container) {
        this.guiService = ServiceLocator.getService(GuiService.class);
        this.chatsService = ServiceLocator.getService(ChatsService.class);
        this.messagesService = ServiceLocator.getService(MessagesService.class);
        this.userService = ServiceLocator.getService(UserService.class);
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

        selectedFileLabel.managedProperty().bind(selectedFileLabel.visibleProperty());
        removeSelectedFileBtn.managedProperty().bind(removeSelectedFileBtn.visibleProperty());
        selectedFileLabel.setVisible(false);
        removeSelectedFileBtn.setVisible(false);

        guiService.subscribe(ChatSelected.class, (e) -> setSelectedChat(e.getChat()));
        guiService.subscribe(MessageReceived.class, (e) -> {
            Platform.runLater(() -> {
                prependMessage(e.getMessage());
            });
        });
        guiService.subscribe(ChatSettingsSaved.class, (e) -> setGpgKeyLabel());
        guiService.subscribe(FileUpdated.class, (e) -> {
            Platform.runLater(() -> {
                handleFileUpdated(e);
            });
        });
        logger.debug("{} initialized", ChatPageController.class.getSimpleName());
    }

    public void closeChat() {
        chatsService.setSelectedChat(null);
    }

    private void setSelectedChat(TdApi.Chat selectedChat) {
        if (selectedChat == null) {
            setVisible(false);
        } else {
            setGpgKeyLabel();
            chatContent.getChildren().removeAll(chatContent.getChildren());
            chatTitleLabel.setText(selectedChat.title);
            scrollToBottom = true;
            var writableChat = selectedChat.permissions.canSendBasicMessages;
            UiUtils.setVisible(sendMessageBox, writableChat);
            setVisible(true);
        }
    }

    private void setGpgKeyLabel() {
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

    private void handleFileUpdated(FileUpdated update) {
        for (Node node : chatContent.getChildrenUnmodifiable()) {
            if (node instanceof VBox bubble) {
                if (bubble.getChildren().size() == 1) {
                    var msgBox = bubble.getChildren().get(0);
                    if (msgBox instanceof MessageDocumentBox docBox) {
                        if (docBox.updateFile(update.getUpdateFile())) {
                            return;
                        }
                    }
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
                        appendMessage(message);
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

    private VBox getMessageBubble(TdApi.Message message) {
        VBox bubble = new VBox();
        bubble.getStyleClass().add("message-bubble");
        if (message.senderId instanceof TdApi.MessageSenderUser senderUser && senderUser.userId == userService.getMyId()) {
            bubble.getStyleClass().add("my-message");
        } else {
            addSenderLabel(bubble, message.senderId);
        }
        var content = getMessageContentBox(message.content);
        if (content instanceof MessageGpgTextBox || content instanceof MessageGpgDocumentBox) {
            bubble.getStyleClass().add("gpg-message");
        }
        bubble.getChildren().add(content);
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
            return chatsService.getChat(senderChat.chatId);
        } else if (sender instanceof TdApi.MessageSenderUser senderUser) {
            return chatsService.getChat(senderUser.userId);
        }
        return null;
    }

    @FXML
    private void openChatSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(ChatSettingsDialogController.class.getResource("/view/chat-settings-dialog.fxml"));
            Parent root = loader.load();
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
