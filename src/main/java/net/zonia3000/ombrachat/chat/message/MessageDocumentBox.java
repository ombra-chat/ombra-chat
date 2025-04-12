package net.zonia3000.ombrachat.chat.message;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiUtils;
import net.zonia3000.ombrachat.components.SelectableText;
import net.zonia3000.ombrachat.services.TelegramClientService;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDocumentBox extends VBox implements FileBox {

    private static final Logger logger = LoggerFactory.getLogger(MessageDocumentBox.class);

    private final TelegramClientService clientService;

    private Label label;
    private Button downloadFileButton;
    private Button openFileButton;
    private SelectableText captionLabel;
    private TdApi.File telegramFile;

    public MessageDocumentBox(TdApi.MessageDocument messageDocument) {
        clientService = ServiceLocator.getService(TelegramClientService.class);
        telegramFile = messageDocument.document.document;
        initChildren(messageDocument);
    }

    private void initChildren(TdApi.MessageDocument messageDocument) {
        var hbox = new HBox();
        label = new Label();
        label.setText(messageDocument.document.fileName);
        label.setMaxWidth(10000);
        HBox.setHgrow(label, Priority.ALWAYS);

        downloadFileButton = new Button();
        downloadFileButton.setText("Download");
        downloadFileButton.setOnAction(event -> downloadFile(messageDocument));

        openFileButton = new Button();
        openFileButton.setText("Open");
        openFileButton.setOnAction(event -> openFile());

        hbox.getChildren().add(label);
        hbox.getChildren().add(downloadFileButton);
        hbox.getChildren().add(openFileButton);

        HBox.setHgrow(hbox, Priority.ALWAYS);
        getChildren().add(hbox);

        if (messageDocument.caption != null) {
            captionLabel = new SelectableText();
            captionLabel.setText(messageDocument.caption.text);
            getChildren().add(captionLabel);
        }

        setButtonsVisibility(messageDocument.document.document);
    }

    private void setButtonsVisibility(TdApi.File document) {
        if (document.local.isDownloadingCompleted) {
            UiUtils.setVisible(openFileButton, true);
            UiUtils.setVisible(downloadFileButton, false);
        } else if (document.local.isDownloadingActive) {
            UiUtils.setVisible(openFileButton, false);
            showDownloadingLabel();
        } else if (document.local.canBeDownloaded) {
            UiUtils.setVisible(openFileButton, false);
            UiUtils.setVisible(downloadFileButton, true);
        } else {
            UiUtils.setVisible(openFileButton, false);
            UiUtils.setVisible(downloadFileButton, false);
        }
    }

    private void downloadFile(TdApi.MessageDocument messageDocument) {
        var downloadRequest = new TdApi.DownloadFile();
        downloadRequest.fileId = messageDocument.document.document.id;
        downloadRequest.priority = 1;
        logger.debug("Starting file download");
        clientService.sendClientMessage(downloadRequest);
        showDownloadingLabel();
    }

    @Override
    public boolean updateFile(TdApi.UpdateFile update) {
        if (telegramFile.id != update.file.id) {
            return false;
        }
        logger.debug("Received file update");
        telegramFile = update.file;
        setButtonsVisibility(update.file);
        return true;
    }

    private void showDownloadingLabel() {
        UiUtils.setVisible(downloadFileButton, true);
        downloadFileButton.setDisable(true);
        downloadFileButton.setText("Downloading...");
    }

    private void openFile() {
        if (Desktop.isDesktopSupported()) {
            new Thread(() -> {
                try {
                    var filePath = telegramFile.local.path;
                    logger.debug("Opening file {}", filePath);
                    Desktop.getDesktop().open(new File(filePath));
                } catch (IOException ex) {
                    logger.error("Error opening file", ex);
                }
            }).start();
        } else {
            logger.error("Desktop is not supported on this platform.");
        }
    }
}
