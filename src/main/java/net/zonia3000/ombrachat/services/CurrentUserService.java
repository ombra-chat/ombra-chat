package net.zonia3000.ombrachat.services;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.zonia3000.ombrachat.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentUserService {

    private static final Logger logger = LoggerFactory.getLogger(CurrentUserService.class);

    private long myId;
    private char[] encryptionPassword;
    private boolean deleteAllData;

    public long getMyId() {
        return myId;
    }

    public void setMyId(long myId) {
        this.myId = myId;
    }

    public void setEncryptionPassword(String encryptionPassword) {
        this.encryptionPassword = encryptionPassword.toCharArray();
    }

    public char[] getEncryptionPassword() {
        var password = encryptionPassword;
        // we use this only once, then we can remove the data from memory
        encryptionPassword = null;
        return password;
    }

    /**
     * Needs to be called before logging out, to tell the service if all the
     * data needs to be removed after the logout is performed.
     *
     * @param deleteAllData if set to true, the service will erase the
     * application folder and the settings after the logout.
     */
    public void setDeleteAllData(boolean deleteAllData) {
        this.deleteAllData = deleteAllData;
    }

    /**
     * Performs cleanup after the logout event is received.
     */
    public void performCleanupAfterLogout() {
        if (!this.deleteAllData) {
            return;
        }

        var settings = ServiceLocator.getService(SettingsService.class);

        logger.debug("Deleting application folder");
        deleteDirectoryRecursively(settings.getApplicationFolderPath());

        logger.debug("Deleting settings");
        settings.deletePreferences();
    }

    public static void deleteDirectoryRecursively(String folderPath) {
        Path directory = Paths.get(folderPath);
        if (Files.exists(directory)) {
            try {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                    for (Path entry : stream) {
                        if (Files.isDirectory(entry)) {
                            deleteDirectoryRecursively(entry.toString());
                        }
                        try {
                            Files.delete(entry);
                        } catch (IOException ex) {
                            logger.error("Error deleting file {}", entry, ex);
                        }
                    }
                }
                // Finally, delete the directory itself
                Files.delete(directory);
            } catch (IOException ex) {
                logger.error("Error deleting directory {}", directory, ex);
            }
        } else {
            logger.warn("Directory does not exist {}", directory);
        }
    }
}
