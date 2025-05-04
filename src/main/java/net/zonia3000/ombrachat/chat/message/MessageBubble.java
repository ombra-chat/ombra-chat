package net.zonia3000.ombrachat.chat.message;

import java.io.IOError;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.application.Platform;
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
import net.zonia3000.ombrachat.services.CurrentUserService;
import net.zonia3000.ombrachat.services.EffectsService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import net.zonia3000.ombrachat.services.UsersService;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageBubble extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(MessageBubble.class);

    private final ChatsService chatsService;
    private final UsersService usersService;
    private final CurrentUserService currentUserService;
    private final TelegramClientService clientService;
    private final EffectsService effectsService;

    private volatile TdApi.Message message;

    private final Label senderLabel = new Label();
    private final Button actionsButton = new Button();
    private VBox contentBox;
    private HBox reactionsBox;

    private boolean read;
    private boolean processingRead;
    private boolean my;
    private boolean gpg;

    public MessageBubble(TdApi.Message message) {
        this.chatsService = ServiceLocator.getService(ChatsService.class);
        this.usersService = ServiceLocator.getService(UsersService.class);
        this.currentUserService = ServiceLocator.getService(CurrentUserService.class);
        this.clientService = ServiceLocator.getService(TelegramClientService.class);
        this.effectsService = ServiceLocator.getService(EffectsService.class);
        this.message = message;
        getStyleClass().add("message-bubble");
        setMy(message.senderId instanceof TdApi.MessageSenderUser senderUser && senderUser.userId == currentUserService.getMyId());
        initHeader();
    }

    public void updateMessage(TdApi.Message message) {
        this.message = message;
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
        setSender();
    }

    public TdApi.Message getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        if (read && !this.read) {
            getStyleClass().removeAll("unread-message");
        } else if (!read) {
            getStyleClass().add("unread-message");
        }
        this.read = read;
        if (read) {
            this.processingRead = false;
        }
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
        this.contentBox = content;
        setGpg(content instanceof MessageGpgTextBox || content instanceof MessageGpgDocumentBox);
        getChildren().add(content);
        setFooter();
    }

    public VBox getContentBox() {
        return contentBox;
    }

    public boolean isFrom(TdApi.User user) {
        if (message.senderId instanceof TdApi.MessageSenderUser senderUser) {
            return senderUser.userId == user.id;
        }
        return false;
    }

    private void setFooter() {
        VBox footer = new VBox();
        footer.setMaxWidth(10000);
        HBox.setHgrow(footer, Priority.ALWAYS);

        HBox firstRow = new HBox();
        Label dateLabel = new Label();
        dateLabel.setText(formatDate());
        dateLabel.setMaxWidth(10000);
        dateLabel.getStyleClass().add("msg-date");
        HBox.setHgrow(dateLabel, Priority.ALWAYS);
        firstRow.getChildren().add(dateLabel);
        footer.getChildren().add(firstRow);

        reactionsBox = new HBox();
        if (message.interactionInfo != null && message.interactionInfo.reactions != null) {
            setReactions(message.interactionInfo.reactions.reactions);
        }
        footer.getChildren().add(reactionsBox);

        getChildren().add(footer);
    }

    private String formatDate() {
        Date currentDate = new Date(1000l * message.date);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return formatter.format(currentDate);
    }

    public void setReactions(TdApi.MessageReaction[] reactions) {
        reactionsBox.getChildren().clear();
        if (reactions == null) {
            return;
        }
        for (var reaction : reactions) {
            if (reaction.type instanceof TdApi.ReactionTypeEmoji reactionType) {
                effectsService.loadReactionImage(reactionType.emoji, 18, (background) -> {
                    Platform.runLater(() -> {
                        Button button = new Button();
                        button.setBackground(background);
                        if (reaction.isChosen) {
                            button.getStyleClass().add("reaction-btn");
                            button.setOnAction((e) -> {
                                removeReaction(reactionType);
                            });
                        }
                        reactionsBox.getChildren().add(button);
                    });
                });
            }
        }
    }

    private void removeReaction(TdApi.ReactionTypeEmoji reactionType) {
        logger.debug("Removing reaction {}", reactionType.emoji);
        clientService.sendClientMessage(new TdApi.RemoveMessageReaction(
                message.chatId,
                message.id,
                reactionType
        ));
    }

    private void setSender() {
        var sender = message.senderId;
        if (sender instanceof TdApi.MessageSenderChat senderChat) {
            var chat = chatsService.getChat(senderChat.chatId);
            if (chat != null) {
                setSender(chat.title);
            }
        } else if (sender instanceof TdApi.MessageSenderUser senderUser) {
            var chat = chatsService.getChat(senderUser.userId);
            if (chat != null) {
                setSender(chat.title);
            } else {
                var userLabel = usersService.getUserDisplayText(senderUser.userId);
                if (userLabel != null) {
                    setSender(userLabel);
                }
            }
        }
    }

    public void setSender(String title) {
        senderLabel.setText(title);
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
            UiUtils.setAppIcon(newStage);
            newStage.setTitle("Message");
            newStage.setScene(scene);
            newStage.showAndWait();
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }
}
