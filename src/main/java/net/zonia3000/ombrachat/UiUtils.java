package net.zonia3000.ombrachat;

import java.io.IOException;
import java.util.Properties;
import javafx.scene.Scene;

public class UiUtils {

    public static void setCommonCss(Scene scene) {
        scene.getStylesheets().add(UiUtils.class.getResource("/view/common.css").toExternalForm());
    }

    public static String getVersion() {
        Properties props = new Properties();
        try {
            props.load(UiUtils.class.getResourceAsStream("/version.properties"));
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return ((String) props.get("ombrachat.version"));
    }
}
