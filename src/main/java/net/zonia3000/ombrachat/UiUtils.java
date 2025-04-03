package net.zonia3000.ombrachat;

import java.io.IOException;
import java.util.Properties;
import javafx.scene.Node;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UiUtils {

    private static final Logger logger = LoggerFactory.getLogger(UiUtils.class);

    public static void setCommonCss(Scene scene) {
        scene.getStylesheets().add(UiUtils.class.getResource("/view/common.css").toExternalForm());
    }

    public static String getVersion() {
        Properties props = new Properties();
        try {
            props.load(UiUtils.class.getResourceAsStream("/version.properties"));
        } catch (IOException ex) {
            logger.error("Error retrieving version", ex);
        }
        return ((String) props.get("ombrachat.version"));
    }

    public static void setVisible(Node node, boolean visible) {
        node.setManaged(visible);
        node.setVisible(visible);
    }
}
