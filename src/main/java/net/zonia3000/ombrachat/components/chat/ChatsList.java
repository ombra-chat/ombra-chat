package net.zonia3000.ombrachat.components.chat;

import java.io.IOError;
import java.io.IOException;
import java.util.Collection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import net.zonia3000.ombrachat.ChatsLoader;
import net.zonia3000.ombrachat.MessagesLoader;
import org.drinkless.tdlib.TdApi;

public class ChatsList extends ListView<TdApi.Chat> {

    private ChatsLoader chatsLoader;
    private MessagesLoader messagesLoader;

    public ChatsList() {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatFoldersBox.class.getResource("/view/chats-list.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException ex) {
            throw new IOError(ex);
        }

        setCellFactory(lv -> new CustomListCell());

        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                messagesLoader.setSelectedChat(newValue);
            }
        });
    }

    public void setLoaders(ChatsLoader chatsLoader, MessagesLoader messagesLoader) {
        this.chatsLoader = chatsLoader;
        this.messagesLoader = messagesLoader;
        this.chatsLoader.setChatsListConsumer(this::setChatsList);
    }

    public void setChatsList(Collection<TdApi.Chat> collection) {
        ObservableList<TdApi.Chat> items = FXCollections.observableArrayList();
        for (var chat : collection) {
            items.add(chat);
        }
        this.setItems(items);
    }

    public static class CustomListCell extends ListCell<TdApi.Chat> {

        @Override
        protected void updateItem(TdApi.Chat item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                // Create an ImageView
                /*Image image = new Image(item.getImageUrl());
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(20);
                imageView.setFitHeight(20);*/

                Label textLabel = new Label(item.title);
                textLabel.setAlignment(Pos.CENTER_LEFT);
                textLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
                textLabel.setMaxWidth(Integer.MAX_VALUE);

                Label numberLabel = new Label(String.valueOf(item.unreadCount));
                numberLabel.setFont(new Font(15));
                numberLabel.setAlignment(Pos.CENTER);
                numberLabel.setStyle("-fx-text-fill: blue;");

                HBox hBox = new HBox(10);
                hBox.getChildren().addAll(textLabel, numberLabel);
                hBox.setAlignment(Pos.CENTER);
                hBox.setPrefHeight(40);
                hBox.setPrefWidth(1);

                HBox.setHgrow(textLabel, Priority.ALWAYS);

                setGraphic(hBox);
            }
        }
    }
}
