package net.zonia3000.ombrachat.services;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class SettingsService {

    private static final String API_ID = "api_id";
    private static final String API_HASH = "api_hash";
    private static final String CHATS = "chats";
    private static final String DEFAULT_FOLDER = "default_folder";

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

    public String getChatKey(long chatId) {
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

    public String setChatKey(long chatId, String chatKey) {
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

    private Preferences getPreferences() {
        return Preferences.userNodeForPackage(SettingsService.class);
    }
}
