package net.zonia3000.ombrachat.services;

import java.io.File;
import net.zonia3000.ombrachat.FileUtils;
import net.zonia3000.ombrachat.ServiceLocator;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentUserService {

    private static final Logger logger = LoggerFactory.getLogger(CurrentUserService.class);

    private long myId;
    private TdApi.User myUser;
    private char[] encryptionPassword;
    private boolean deleteAllData;

    public long getMyId() {
        return myId;
    }

    public void setMyId(long myId) {
        this.myId = myId;
    }

    public TdApi.User getMyUser() {
        return myUser;
    }

    public void setMyUser(TdApi.User myUser) {
        this.myUser = myUser;
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
        FileUtils.deleteDirectoryRecursively(new File(settings.getApplicationFolderPath()));

        logger.debug("Deleting settings");
        settings.deletePreferences();
    }
}
