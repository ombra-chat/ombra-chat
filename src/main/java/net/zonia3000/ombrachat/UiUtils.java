package net.zonia3000.ombrachat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
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

    public static void setAppIcon(Stage primaryStage) {
        try (InputStream in = UiUtils.class.getResourceAsStream("/view/icons/ombra-chat-logo.png")) {
            var image = new Image(in);
            primaryStage.getIcons().add(image);
        } catch (IOException ex) {
            logger.error("Unable to initialize app icon", ex);
        }
    }
}
