package net.zonia3000.ombrachat;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static boolean deleteDirectoryRecursively(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectoryRecursively(file);
            }
        }
        boolean deleted = directoryToBeDeleted.delete();
        if (!deleted) {
            logger.warn("Unable to delete file {}", directoryToBeDeleted.getAbsolutePath());
        }
        return deleted;
    }
}
