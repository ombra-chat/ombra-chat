package net.zonia3000.ombrachat.controllers;

import java.io.IOError;
import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.EffectsService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDialogController {

    private static final Logger logger = LoggerFactory.getLogger(MessageDialogController.class);

    private static final int REACTION_SIZE = 18;

    private TdApi.Message message;
    private TdApi.Chat chat;
    private EffectsService effectsService;
    private TelegramClientService clientService;

    @FXML
    private Button deleteMessageBtn;
    @FXML
    private FlowPane reactionsPane;

    private TdApi.AvailableReactions availableReactions;

    public void setMessage(TdApi.Message message) {
        this.message = message;
        chat = ServiceLocator.getService(ChatsService.class).getSelectedChat();
        if (!chat.canBeDeletedForAllUsers && !chat.canBeDeletedOnlyForSelf) {
            this.deleteMessageBtn.setDisable(true);
        }
        this.effectsService = ServiceLocator.getService(EffectsService.class);
        this.clientService = ServiceLocator.getService(TelegramClientService.class);
        loadAvailableReactions();
    }

    private void loadAvailableReactions() {
        var telegramClientService = ServiceLocator.getService(TelegramClientService.class);
        telegramClientService.sendClientMessage(
                new TdApi.GetMessageAvailableReactions(chat.id, message.id, 5),
                (r) -> {
                    if (r instanceof TdApi.AvailableReactions reactions) {
                        Platform.runLater(() -> {
                            this.availableReactions = reactions;
                            logger.debug("Available message reactions: {}", reactions.topReactions.length);
                            reactionsPane.getChildren().clear();
                            for (var reaction : reactions.topReactions) {
                                if (reaction.type instanceof TdApi.ReactionTypeEmoji emoji) {
                                    addReactionButton(emoji.emoji);
                                }
                            }
                        });
                    }
                }
        );
    }

    private void addReactionButton(String emoji) {
        var button = new Button();
        button.setMinWidth(REACTION_SIZE);
        button.setMinHeight(REACTION_SIZE);
        UiUtils.setVisible(button, false);
        effectsService.loadReactionImage(emoji, REACTION_SIZE, (reactionBackground) -> {
            Platform.runLater(() -> {
                button.setBackground(reactionBackground);
                button.getStyleClass().add("reaction-btn");
                button.setOnAction((e) -> {
                    addReactionToMessage(emoji);
                });
                UiUtils.setVisible(button, true);
            });
        });
        reactionsPane.getChildren().add(button);
    }

    private void addReactionToMessage(String emoji) {
        logger.debug("Adding {} reaction", emoji);
        clientService.sendClientMessage(new TdApi.AddMessageReaction(
                chat.id,
                message.id,
                getEmojiReaction(emoji),
                false,
                false
        ));
        closeDialog();
    }

    private TdApi.ReactionTypeEmoji getEmojiReaction(String emoji) {
        for (var reaction : availableReactions.topReactions) {
            if (reaction.type instanceof TdApi.ReactionTypeEmoji emojiType) {
                if (emojiType.emoji.equals(emoji)) {
                    return emojiType;
                }
            }
        }
        return null;
    }

    @FXML
    private void deleteMessage() {
        var client = ServiceLocator.getService(TelegramClientService.class);
        logger.debug("Deleting message {}", message.id);
        client.sendClientMessage(new TdApi.DeleteMessages(chat.id, new long[]{message.id}, !chat.canBeDeletedOnlyForSelf));
        closeDialog();
    }

    @FXML
    private void forwardMessage() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MessageDialogController.class.getResource("/view/forward-dialog.fxml"));
            Parent root = loader.load();
            ((ForwardDialogController) loader.getController()).setMessageToForward(message);
            Scene scene = new Scene(root);
            UiUtils.setCommonCss(scene);
            Stage stage = (Stage) deleteMessageBtn.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }

    private void closeDialog() {
        Stage stage = (Stage) deleteMessageBtn.getScene().getWindow();
        stage.close();
    }
}
