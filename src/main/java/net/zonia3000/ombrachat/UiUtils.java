package net.zonia3000.ombrachat;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UiUtils {

    private static final Logger logger = LoggerFactory.getLogger(UiUtils.class);

    private static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");

    /**
     * Lazy initialized.
     */
    private static Clipboard clip = null;

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

    public static void setAppIcon(Stage stage) {
        var icon = getAppIcon();
        if (icon != null) {
            stage.getIcons().add(icon);
        }
    }

    public static Image getAppIcon() {
        try (InputStream in = UiUtils.class.getResourceAsStream("/view/icons/ombra-chat-logo.png")) {
            return new Image(in);
        } catch (IOException ex) {
            logger.error("Unable to initialize app icon", ex);
            return null;
        }
    }

    /**
     * Supports Linux copy and paste using middle mouse button (primary
     * clipboard quick copy). Causes the following Gdk-WARNING:
     * XSetErrorHandler() called with a GDK error trap pushed.
     */
    public static void setupMiddleButtonPasteHandler(Node node) {
        if (!IS_LINUX) {
            return;
        }
        node.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.isMiddleButtonDown()) {
                final TextField field = (TextField) event.getSource();
                try {
                    if (clip == null) {
                        clip = Toolkit.getDefaultToolkit().getSystemSelection();
                    }
                    if (clip.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                        var text = clip.getData(DataFlavor.stringFlavor).toString();
                        field.insertText(field.getCaretPosition(), text);
                    }
                } catch (IOException | UnsupportedFlavorException ex) {
                    logger.warn("Cannot read middle button paste buffer", ex);
                }
            }
        });
    }
}
