package net.zonia3000.ombrachat;

import java.util.prefs.Preferences;

public class Settings {

    private static final String API_ID = "api_id";
    private static final String API_HASH = "api_hash";

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

    private Preferences getPreferences() {
        return Preferences.userNodeForPackage(Settings.class);
    }
}
