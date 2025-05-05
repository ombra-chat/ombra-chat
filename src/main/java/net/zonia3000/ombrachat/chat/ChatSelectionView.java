package net.zonia3000.ombrachat.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.services.GuiService;
import org.drinkless.tdlib.TdApi;

public class ChatSelectionView extends ListView<TdApi.Chat> {

    private final GuiService guiService;

    private Consumer<TdApi.Chat> chatConsumer;

    public ChatSelectionView() {
        guiService = ServiceLocator.getService(GuiService.class);

        setCellFactory(lv -> new CustomListCell());

        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && chatConsumer != null) {
                chatConsumer.accept(newValue);
            }
        });
    }

    public void setAction(Consumer<TdApi.Chat> chatConsumer) {
        this.chatConsumer = chatConsumer;
    }

    public void setChatsList(Collection<TdApi.Chat> collection) {
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
                List<Node> rowNodes = new ArrayList<>();

                if (chat.type instanceof TdApi.ChatTypeSecret && guiService.getLockImage() != null) {
                    ImageView imageView = new ImageView(guiService.getLockImage());
                    imageView.setFitWidth(24);
                    imageView.setFitHeight(24);
                    rowNodes.add(imageView);
                }

                Label textLabel = new Label(chat.title);
                textLabel.setAlignment(Pos.CENTER_LEFT);
                textLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
                textLabel.setMaxWidth(Integer.MAX_VALUE);
                rowNodes.add(textLabel);

                HBox hBox = new HBox(10);
                hBox.getChildren().addAll(rowNodes);
                hBox.setAlignment(Pos.CENTER);
                hBox.setPrefHeight(40);
                hBox.setPrefWidth(1);

                HBox.setHgrow(textLabel, Priority.ALWAYS);

                setGraphic(hBox);
            }
        }
    }
}
