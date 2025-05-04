package net.zonia3000.ombrachat.services;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javax.imageio.ImageIO;
import net.zonia3000.ombrachat.ServiceLocator;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads images for reactions and stickers.
 */
public class EffectsService {

    private static final Logger logger = LoggerFactory.getLogger(EffectsService.class);

    private final TelegramClientService clientService;

    private static final Map<String, TdApi.File> allReactions = new ConcurrentHashMap<>();

    public EffectsService() {
        this.clientService = ServiceLocator.getService(TelegramClientService.class);
    }

    public boolean onResult(TdApi.Object object) {
        if (object instanceof TdApi.UpdateAvailableMessageEffects update) {
            return handleUpdateAvailableMessageEffects(update);
        }
        return false;
    }

    public boolean handleUpdateAvailableMessageEffects(TdApi.UpdateAvailableMessageEffects update) {
        logger.debug("Updating message effects");
        for (var reactionId : update.reactionEffectIds) {
            clientService.sendClientMessage(
                    new TdApi.GetMessageEffect(reactionId),
                    (r) -> {
                        if (r instanceof TdApi.MessageEffect messageEffect) {
                            var emoji = messageEffect.emoji;
                            var file = messageEffect.staticIcon.sticker;
                            allReactions.put(emoji, file);
                        }
                    });
        }
        return true;
    }

    public void loadReactionImage(String emoji, int size, Consumer<Background> backgroundConsumer) {
        var telegramFile = allReactions.get(emoji);
        if (telegramFile.local.isDownloadingCompleted) {
            backgroundConsumer.accept(getReactionBackground(telegramFile, size));
        } else if (telegramFile.local.canBeDownloaded && !telegramFile.local.isDownloadingActive) {
            clientService.sendClientMessage(new TdApi.DownloadFile(telegramFile.id, 1, 0, 0, false),
                    (TdApi.Object object) -> {
                        if (object instanceof TdApi.File file) {
                            if (file.local.isDownloadingCompleted) {
                                backgroundConsumer.accept(getReactionBackground(file, size));
                            } else {
                                loadReactionImage(emoji, size, backgroundConsumer);
                            }
                        }
                    });
        }

    }

    private Background getReactionBackground(TdApi.File file, int size) {
        String path = getReadableFilePath(file.local.path);
        Image img = new Image("file:" + path);
        BackgroundImage bgImg = new BackgroundImage(
                img,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(size, size, false, false, false, false)
        );
        return new Background(bgImg);
    }

    /**
     * Convert webp to png if needed.
     */
    private String getReadableFilePath(String path) {
        if (!path.endsWith(".webp")) {
            return path;
        }
        var pngPath = path.substring(0, path.lastIndexOf('.')) + ".png";
        var pngFile = new File(pngPath);
        if (!pngFile.exists()) {
            try {
                logger.debug("converting {} to png", path);
                var webpImage = ImageIO.read(new File(path));
                ImageIO.write(webpImage, "png", pngFile);
            } catch (IOException ex) {
                logger.error("Unable to convert webp to png", ex);
            }
        }
        return pngPath;
    }
}
