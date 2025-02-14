package net.zonia3000.ombrachat.components.chat.message;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

public class MessagePhotoBox extends VBox {

    private final Client client;

    public MessagePhotoBox(TdApi.MessagePhoto messagePhoto, Client client) {
        this.client = client;

        setPhoto(messagePhoto);

        if (messagePhoto.caption != null) {
            Label textLabel = new Label(messagePhoto.caption.text);
            textLabel.setWrapText(true);
            getChildren().add(textLabel);
        }
    }

    private void setPhoto(TdApi.MessagePhoto messagePhoto) {
        if (messagePhoto.photo.sizes.length > 0) {
            var size = messagePhoto.photo.sizes[0];
            var photo = size.photo;
            setPhoto(messagePhoto, photo, size);
        }
    }

    private void setPhoto(TdApi.MessagePhoto messagePhoto, TdApi.File photo, TdApi.PhotoSize size) {
        if (photo.local.isDownloadingCompleted) {
            Platform.runLater(() -> {
                Image image = new Image("file:" + photo.local.path);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(size.width);
                imageView.setFitHeight(size.height);
                imageView.setPreserveRatio(true);
                imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    var largerSize = messagePhoto.photo.sizes[messagePhoto.photo.sizes.length - 1];
                    openImageInNewWindow(largerSize.photo, largerSize);
                });
                getChildren().addFirst(imageView);
            });
        } else {
            client.send(new TdApi.DownloadFile(photo.id, 1, 0, 0, false), new Client.ResultHandler() {
                @Override
                public void onResult(TdApi.Object object) {
                    if (object instanceof TdApi.File file) {
                        setPhoto(messagePhoto, file, size);
                    }
                }
            });
        }
    }

    private void openImageInNewWindow(TdApi.File photo, TdApi.PhotoSize size) {
        if (photo.local.isDownloadingCompleted) {
            Platform.runLater(() -> {
                Stage newStage = new Stage();

                Image image = new Image("file:" + photo.local.path);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(size.width);
                imageView.setFitHeight(size.height);
                imageView.setPreserveRatio(true);

                VBox newLayout = new VBox(imageView);

                Scene newScene = new Scene(newLayout);
                newStage.setScene(newScene);
                newStage.setTitle("Image Viewer");
                newStage.show();
            });
        } else {
            client.send(new TdApi.DownloadFile(photo.id, 1, 0, 0, false), new Client.ResultHandler() {
                @Override
                public void onResult(TdApi.Object object) {
                    if (object instanceof TdApi.File file) {
                        openImageInNewWindow(file, size);
                    }
                }
            });
        }
    }
}
