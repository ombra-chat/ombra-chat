package net.zonia3000.ombrachat.events;

import org.drinkless.tdlib.TdApi;

public class FileUpdated implements Event {

    private final TdApi.UpdateFile updateFile;

    public FileUpdated(TdApi.UpdateFile updateFile) {
        this.updateFile = updateFile;
    }

    public TdApi.UpdateFile getUpdateFile() {
        return updateFile;
    }
}
