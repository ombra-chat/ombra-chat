package net.zonia3000.ombrachat.chat;

import java.util.Collection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.events.ChatsListUpdated;
import net.zonia3000.ombrachat.services.ChatsService;
import net.zonia3000.ombrachat.services.GuiService;
import org.drinkless.tdlib.TdApi;

public class ChatsListView extends ListView<TdApi.Chat> {

    private TdApi.Chat lastSelectedChat;

    private final GuiService guiService;
    private final ChatsService chatsService;

    public ChatsListView() {
        guiService = ServiceLocator.getService(GuiService.class);
        chatsService = ServiceLocator.getService(ChatsService.class);

        guiService.subscribe(ChatsListUpdated.class, (e) -> setChatsList(e.getChats()));

        setCellFactory(lv -> new CustomListCell());

        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                lastSelectedChat = newValue;
                chatsService.setSelectedChat(newValue);
            }
        });

        chatsService.loadChats();
    }

    private void setChatsList(Collection<TdApi.Chat> collection) {
        ObservableList<TdApi.Chat> items = FXCollections.observableArrayList();
        for (var chat : collection) {
            items.add(chat);
        }
        this.setItems(items);
    }

    public class CustomListCell extends ListCell<TdApi.Chat> {

        @Override
        protected void updateItem(TdApi.Chat chat, boolean empty) {
            super.updateItem(chat, empty);
            if (empty || chat == null) {
                setText(null);
                setGraphic(null);
            } else {
                // Create an ImageView
                /*Image image = new Image(item.getImageUrl());
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(20);
                imageView.setFitHeight(20);*/

                Label textLabel = new Label(chat.title);
                textLabel.setAlignment(Pos.CENTER_LEFT);
                textLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
                textLabel.setMaxWidth(Integer.MAX_VALUE);

                Label numberLabel = new Label(String.valueOf(chat.unreadCount));
                numberLabel.setFont(new Font(15));
                numberLabel.setAlignment(Pos.CENTER);
                if (chat.notificationSettings.muteFor > 0) {
                    numberLabel.setStyle("-fx-text-fill: blue;");
                } else {
                    numberLabel.setStyle("-fx-text-fill: red;");
                }
                UiUtils.setVisible(numberLabel, chat.unreadCount > 0);

                HBox hBox = new HBox(10);
                hBox.getChildren().addAll(textLabel, numberLabel);
                hBox.setAlignment(Pos.CENTER);
                hBox.setPrefHeight(40);
                hBox.setPrefWidth(1);

                HBox.setHgrow(textLabel, Priority.ALWAYS);

                setGraphic(hBox);

                hBox.setOnMouseClicked(event -> {
                    if (chatsService.getSelectedChat() == null && lastSelectedChat != null) {
                        chatsService.setSelectedChat(lastSelectedChat);
                    }
                });
            }
        }
    }
}
