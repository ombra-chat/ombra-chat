package net.zonia3000.ombrachat.chat.message;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.components.SelectableText;
import net.zonia3000.ombrachat.services.TelegramClientService;
import org.drinkless.tdlib.TdApi;

public class MessagePhotoBox extends VBox implements SelectableBox {

    private final TelegramClientService clientService;

    private final SelectableText textLabel = new SelectableText();

    public MessagePhotoBox(TdApi.MessagePhoto messagePhoto) {
        clientService = ServiceLocator.getService(TelegramClientService.class);

        setPhoto(messagePhoto);

        if (messagePhoto.caption != null) {
            textLabel.setText(messagePhoto.caption.text);
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
                imageView.setFitWidth(size.width == 0 ? 320 : size.width);
                imageView.setFitHeight(size.height == 0 ? 320 : size.height);
                imageView.setPreserveRatio(true);
                imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    var largerSize = messagePhoto.photo.sizes[messagePhoto.photo.sizes.length - 1];
                    openImageInNewWindow(largerSize.photo, largerSize);
                });
                getChildren().addFirst(imageView);
            });
        } else {
            clientService.sendClientMessage(new TdApi.DownloadFile(photo.id, 1, 0, 0, false), (TdApi.Object object) -> {
                if (object instanceof TdApi.File file) {
                    setPhoto(messagePhoto, file, size);
                }
            });
        }
    }

    private void openImageInNewWindow(TdApi.File photo, TdApi.PhotoSize size) {
        if (photo.local.isDownloadingCompleted) {
            Platform.runLater(() -> {
                Stage newStage = new Stage();
                UiUtils.setAppIcon(newStage);

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
            clientService.sendClientMessage(new TdApi.DownloadFile(photo.id, 1, 0, 0, false), (TdApi.Object object) -> {
                if (object instanceof TdApi.File file) {
                    openImageInNewWindow(file, size);
                }
            });
        }
    }

    @Override
    public String getSelectedText() {
        return textLabel.getSelectedText();
    }
}
