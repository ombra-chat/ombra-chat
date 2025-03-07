package net.zonia3000.ombrachat;

import javafx.scene.Scene;

public class UiUtils {

    public static void setCommonCss(Scene scene) {
        scene.getStylesheets().add(UiUtils.class.getResource("/view/common.css").toExternalForm());
    }
}
