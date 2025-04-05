package net.zonia3000.ombrachat.chat.message;

import java.io.File;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.services.TelegramClientService;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageGpgTextBox extends VBox implements FileBox {

    private static final Logger logger = LoggerFactory.getLogger(MessageGpgTextBox.class);

    private final TelegramClientService clientService;
    private final GpgService gpgService;

    private Label label;
    private TdApi.File telegramFile;

    public MessageGpgTextBox(TdApi.MessageDocument messageDocument) {
        clientService = ServiceLocator.getService(TelegramClientService.class);
        gpgService = ServiceLocator.getService(GpgService.class);
        telegramFile = messageDocument.document.document;
        initChildren(messageDocument);
    }

    private void initChildren(TdApi.MessageDocument messageDocument) {
        label = new Label();
        label.setText("...");
        getChildren().add(label);

        var document = messageDocument.document.document;
        if (document.local.isDownloadingCompleted) {
            var file = new File(document.local.path);
            var msg = gpgService.decryptToString(file);
            if (msg != null) {
                label.setText(msg);
            }
        } else if (document.local.canBeDownloaded) {
            downloadFile(messageDocument);
        }
    }

    @Override
    public boolean updateFile(TdApi.UpdateFile update) {
        if (telegramFile.id != update.file.id) {
            return false;
        }
        logger.debug("Received file update");
        telegramFile = update.file;
        return true;
    }

    private void downloadFile(TdApi.MessageDocument messageDocument) {
        var downloadRequest = new TdApi.DownloadFile();
        downloadRequest.fileId = messageDocument.document.document.id;
        downloadRequest.priority = 1;
        logger.debug("Starting file download");
        clientService.sendClientMessage(downloadRequest);
    }
}
