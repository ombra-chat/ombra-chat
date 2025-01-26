module net.zonia3000 {
    requires javafx.controls;
    requires javafx.fxml;
    requires tdlib;
    requires java.prefs;
    exports net.zonia3000.ombrachat;
    exports net.zonia3000.ombrachat.login;
    exports net.zonia3000.ombrachat.components.chat;
    opens net.zonia3000.ombrachat to javafx.fxml;
    opens net.zonia3000.ombrachat.login to javafx.fxml;
    opens net.zonia3000.ombrachat.components.chat to javafx.fxml;
}
