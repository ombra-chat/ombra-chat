package net.zonia3000.ombrachat.services;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import net.zonia3000.ombrachat.CryptoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);

    private static final String API_ID = "api_id";
    private static final String API_HASH = "api_hash";
    private static final String CHATS = "chats";
    private static final String DEFAULT_FOLDER = "default_folder";
    private static final String INITIAL_CONFIG_DONE = "initial_config_done";
    private static final String APPLICATION_FOLDER_PATH = "app_folder_path";
    private static final String TDLIB_ENCRYPT = "tdlib_encrypt";
    private static final String TDLIB_ENCRYPTION_PASSWORD_GPG_ENCRYPTED = "tdlib_encryption_password_gpg_encrypted";
    private static final String TDLIB_ENCRYPTION_SALT = "tdlib_encryption_salt";
    private static final String DEFAULT_PUBRING = "default_pubring";

    public static enum EncryptionType {
        NONE,
        PASSWORD,
        GPG
    };

    public int getApiId() {
        return getPreferences().getInt(API_ID, 0);
    }

    public void setApiId(int apiId) {
        getPreferences().putInt(API_ID, apiId);
    }

    public String getApiHash() {
        return getPreferences().get(API_HASH, "");
    }

    public void setApiHash(String apiHash) {
        getPreferences().put(API_HASH, apiHash);
    }

    public boolean isInitialConfigDone() {
        return getPreferences().getBoolean(INITIAL_CONFIG_DONE, false);
    }

    public void setInitialConfigDone(boolean value) {
        getPreferences().putBoolean(INITIAL_CONFIG_DONE, value);
    }

    public String getApplicationFolderPath() {
        return getPreferences().get(APPLICATION_FOLDER_PATH, "ombra-chat");
    }

    public void setApplicationFolderPath(String path) {
        getPreferences().put(APPLICATION_FOLDER_PATH, path);
    }

    public EncryptionType getTdlibDatabaseEncryption() {
        return EncryptionType.valueOf(getPreferences().get(TDLIB_ENCRYPT, EncryptionType.NONE.toString()));
    }

    public void setTdlibDatabaseEncryption(EncryptionType encryptionType) {
        getPreferences().put(TDLIB_ENCRYPT, encryptionType.toString());
    }

    public String getTdlibEncryptedPassword() {
        return getPreferences().get(TDLIB_ENCRYPTION_PASSWORD_GPG_ENCRYPTED, "");
    }

    /**
     * @param encryptedPassword random generated password encrypted with GPG key
     */
    public void setTdlibEncryptedPassword(String encryptedPassword) {
        getPreferences().put(TDLIB_ENCRYPTION_PASSWORD_GPG_ENCRYPTED, encryptedPassword);
    }

    public String getTdlibEncryptionSalt() {
        var preferences = getPreferences();
        var salt = preferences.get(TDLIB_ENCRYPTION_SALT, "");
        if (salt.isBlank()) {
            salt = CryptoUtils.generateRandomSalt();
            preferences.put(TDLIB_ENCRYPTION_SALT, salt);
        }
        return salt;
    }

    public void setEncryptionSalt(String salt) {
        getPreferences().put(TDLIB_ENCRYPTION_SALT, salt);
    }

    public String getChatKeyFingerprint(long chatId) {
        String chatsString = getPreferences().get(CHATS, "");
        for (String part : chatsString.split(";")) {
            String[] items = part.split(":");
            if (items.length == 2) {
                if (items[0].equals(String.valueOf(chatId))) {
                    return items[1];
                }
            }
        }
        return null;
    }

    public String setChatKeyFingerprint(long chatId, String chatKey) {
        String chatsString = getPreferences().get(CHATS, "");
        List<String> chatKeys = new ArrayList<>();
        boolean found = false;
        for (String part : chatsString.split(";")) {
            String[] items = part.split(":");
            if (items.length == 2) {
                if (items[0].equals(String.valueOf(chatId))) {
                    if (chatKey != null) {
                        chatKeys.add(chatId + ":" + chatKey);
                    }
                    found = true;
                } else {
                    chatKeys.add(part);
                }
            }
        }
        if (!found && chatKey != null) {
            chatKeys.add(chatId + ":" + chatKey);
        }
        getPreferences().put(CHATS, String.join(";", chatKeys));
        return null;
    }

    public int getDefaultFolder() {
        return getPreferences().getInt(DEFAULT_FOLDER, 0);
    }

    public void setDefaultFolder(int defaultFolderId) {
        getPreferences().putInt(DEFAULT_FOLDER, defaultFolderId);
    }

    public void deletePreferences() {
        try {
            getPreferences().removeNode();
        } catch (BackingStoreException ex) {
            logger.error("Unable to delete preferences", ex);
        }
    }

    public String getPubringPath() {
        var defaultPubring = Paths.get(System.getProperty("user.home"), ".gnupg", "pubring.kbx");
        return getPreferences().get(DEFAULT_PUBRING, defaultPubring.toFile().getAbsolutePath());
    }

    public void setPubringPath(String path) {
        getPreferences().put(DEFAULT_PUBRING, path);
    }

    private Preferences getPreferences() {
        return Preferences.userNodeForPackage(SettingsService.class);
    }
}
