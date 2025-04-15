package net.zonia3000.ombrachat.chat.message;

import java.io.File;
import javafx.scene.layout.VBox;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.components.SelectableText;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageGpgTextBox extends VBox implements FileBox {

    private static final Logger logger = LoggerFactory.getLogger(MessageGpgTextBox.class);

    private final TelegramClientService clientService;
    private final GpgService gpgService;

    private final SelectableText selectableText = new SelectableText();
    private TdApi.File telegramFile;

    public MessageGpgTextBox(TdApi.MessageDocument messageDocument) {
        clientService = ServiceLocator.getService(TelegramClientService.class);
        gpgService = ServiceLocator.getService(GpgService.class);
        telegramFile = messageDocument.document.document;
        initChildren(messageDocument);
    }

    private void initChildren(TdApi.MessageDocument messageDocument) {
        selectableText.setText("...");
        getChildren().add(selectableText);

        var document = messageDocument.document.document;
        if (document.local.isDownloadingCompleted) {
            decrypt(document);
        } else if (document.local.canBeDownloaded) {
            downloadFile(messageDocument);
        }
    }

    @Override
    public boolean updateFile(TdApi.UpdateFile update) {
        if (telegramFile.id != update.file.id) {
            return false;
        }
        logger.debug("Received file update; id={}", telegramFile.id);
        telegramFile = update.file;
        if (update.file.local.isDownloadingCompleted) {
            decrypt(update.file);
        }
        return true;
    }

    private void decrypt(TdApi.File document) {
        var file = new File(document.local.path);
        var msg = gpgService.decryptToString(file);
        if (msg != null) {
            selectableText.setText(msg);
        }
    }

    private void downloadFile(TdApi.MessageDocument messageDocument) {
        var downloadRequest = new TdApi.DownloadFile();
        downloadRequest.fileId = messageDocument.document.document.id;
        downloadRequest.priority = 1;
        logger.debug("Starting file download; id={}", downloadRequest.fileId);
        clientService.sendClientMessage(downloadRequest);
    }
}
