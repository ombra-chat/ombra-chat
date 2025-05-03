package net.zonia3000.ombrachat.services;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javax.imageio.ImageIO;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThumbnailService {

    private static final Logger logger = LoggerFactory.getLogger(ThumbnailService.class);

    private static final List<String> TEMPORARY_FILES = new CopyOnWriteArrayList<>();

    public TdApi.InputThumbnail createThumbnail(String imagePath) {
        logger.debug("Generating thumbnail for {}", imagePath);
        Image thumbnail = new Image("file:" + imagePath, 320, 320, true, true);
        if (thumbnail.isError()) {
            logger.error("Error creating thumbnail", thumbnail.getException());
            return null;
        }
        int width = (int) thumbnail.getWidth();
        int height = (int) thumbnail.getHeight();
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // read each pixel and copy the ARGB values to the buffered image
        PixelReader pr = thumbnail.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        try {
            File tempFile = File.createTempFile("thumb", ".png");
            ImageIO.write(buffered, "png", tempFile);
            var thumbnailPath = tempFile.getAbsolutePath();
            TEMPORARY_FILES.add(thumbnailPath);
            logger.debug("Thumbnail generated at {}", thumbnailPath);
            var thumnailFileLocal = new TdApi.InputFileLocal(thumbnailPath);
            return new TdApi.InputThumbnail(thumnailFileLocal, width, height);
        } catch (IOException ex) {
            logger.error("Unable to generate thumbnail", ex);
        }
        return null;
    }

    public void removeThumbnail(TdApi.InputMessagePhoto inputMessagePhoto) {
        if (inputMessagePhoto.thumbnail.thumbnail instanceof TdApi.InputFileLocal inputFileLocal) {
            var thumbnailToRemove = inputFileLocal.path;
            logger.debug("Removing thumnail {}", thumbnailToRemove);
            var file = new File(thumbnailToRemove);
            file.delete();
            TEMPORARY_FILES.remove(thumbnailToRemove);
        }
    }

    public void cleanup() {
        logger.debug("Deleting remaining temporary thumbnail files");
        for (var path : TEMPORARY_FILES) {
            var file = new File(path);
            file.delete();
        }
    }
}
