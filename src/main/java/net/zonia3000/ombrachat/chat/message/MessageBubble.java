package net.zonia3000.ombrachat.chat.message;

import java.io.IOError;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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
    private final Label forwaredFromLabel = new Label();
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
        initRepliesBox();
    }

    public void updateMessage(TdApi.Message message) {
        this.message = message;
    }

    private void initHeader() {
        HBox headerBox = new HBox();

        VBox leftBox = new VBox();
        leftBox.setMaxWidth(10000);
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        initSenderLabel(leftBox);
        initForwardLabel(leftBox);
        headerBox.getChildren().add(leftBox);

        initMessageActionsButton(headerBox);

        getChildren().add(headerBox);
    }

    private void initSenderLabel(VBox container) {
        senderLabel.setTextFill(Color.BLUE);
        senderLabel.getStyleClass().add("bold");
        senderLabel.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            // TODO
        });
        senderLabel.setMaxWidth(10000);
        HBox.setHgrow(senderLabel, Priority.ALWAYS);
        senderLabel.setText(getSenderTitle(message.senderId));
        container.getChildren().add(senderLabel);
    }

    private void initMessageActionsButton(HBox container) {
        Button actionsButton = new Button();
        actionsButton.getStyleClass().addAll("btn", "btn-20", "message-action-btn");
        actionsButton.setOnAction((e) -> openMessageDialog());
        container.getChildren().add(actionsButton);
    }

    private void initForwardLabel(VBox container) {
        var forwardInfo = message.forwardInfo;
        if (forwardInfo == null) {
            return;
        }
        HBox headerBox = new HBox();
        Label text = new Label("Forwarded from ");
        if (forwardInfo.source == null) {
            forwaredFromLabel.setText(getSenderTitle(forwardInfo.origin));
        } else {
            forwaredFromLabel.setText(getSenderTitle(forwardInfo.source.senderId));
        }
        forwaredFromLabel.setTextFill(Color.BLUE);
        forwaredFromLabel.getStyleClass().addAll("bold", "pb");
        headerBox.getChildren().add(text);
        headerBox.getChildren().add(forwaredFromLabel);
        container.getChildren().add(headerBox);
    }

    private void initRepliesBox() {
        if (message.replyTo == null) {
            return;
        }

        HBox repliesBox = new HBox();
        repliesBox.getStyleClass().add("replies-box");
        VBox.setMargin(repliesBox, new Insets(0, 0, 5, 0));

        TextFlow textFlow = new TextFlow();
        repliesBox.getChildren().add(textFlow);

        getChildren().add(repliesBox);

        if (message.replyTo instanceof TdApi.MessageReplyToMessage reply) {
            clientService.sendClientMessage(new TdApi.GetRepliedMessage(message.chatId, message.id), (r) -> {
                if (r instanceof TdApi.Message replyToMessage) {
                    var senderTitle = getSenderTitle(replyToMessage.senderId);
                    if (senderTitle != null) {
                        var senderText = new Text(senderTitle + " ");
                        senderText.setFill(Color.BLUE);
                        textFlow.getChildren().add(senderText);
                    }

                    Text text = new Text();
                    if (reply.quote == null) {
                        var textContent = MessageUtils.getTextContent(replyToMessage.content);
                        if (textContent == null || textContent.isBlank()) {
                            textContent = replyToMessage.content.getClass().getCanonicalName();
                        }
                        text.setText(textContent);
                    } else {
                        text.setText(reply.quote.text.text);
                    }
                    textFlow.getChildren().add(text);
                }
            });
        }
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

    public boolean isForwaredFrom(TdApi.User user) {
        if (message.forwardInfo == null || message.forwardInfo.source == null) {
            return false;
        }
        if (message.forwardInfo.source.senderId instanceof TdApi.MessageSenderUser senderUser) {
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

    private String getSenderTitle(TdApi.MessageSender sender) {
        if (sender instanceof TdApi.MessageSenderChat senderChat) {
            var chat = chatsService.getChat(senderChat.chatId);
            if (chat != null) {
                return chat.title;
            }
        } else if (sender instanceof TdApi.MessageSenderUser senderUser) {
            return getSenderTitle(senderUser.userId);
        }
        logger.warn("Sender title is null {}", sender);
        return null;
    }

    private String getSenderTitle(TdApi.MessageOrigin origin) {
        if (origin instanceof TdApi.MessageOriginChannel originChannel) {
            var chat = chatsService.getChat(originChannel.chatId);
            if (chat != null) {
                return chat.title;
            }
            return originChannel.authorSignature;
        } else if (origin instanceof TdApi.MessageOriginChat originChat) {
            var chat = chatsService.getChat(originChat.senderChatId);
            if (chat != null) {
                return chat.title;
            }
        } else if (origin instanceof TdApi.MessageOriginHiddenUser originHiddenUser) {
            return originHiddenUser.senderName;
        } else if (origin instanceof TdApi.MessageOriginUser originUser) {
            return getSenderTitle(originUser.senderUserId);
        }
        logger.warn("Sender title is null {}", origin);
        return null;
    }

    private String getSenderTitle(long userId) {
        var chat = chatsService.getChat(userId);
        if (chat != null) {
            return chat.title;
        } else {
            var userLabel = usersService.getUserDisplayText(userId);
            if (userLabel != null) {
                return userLabel;
            }
        }
        return null;
    }

    public void setSender(String title) {
        senderLabel.setText(title);
    }

    public void setForwardedFrom(String title) {
        forwaredFromLabel.setText(title);
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
