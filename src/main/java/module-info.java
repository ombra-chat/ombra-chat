module ombrachat {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.drinkless.tdlib;
    requires java.prefs;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pg;
    exports net.zonia3000.ombrachat;
    exports net.zonia3000.ombrachat.login;
    exports net.zonia3000.ombrachat.components.chat;
    opens net.zonia3000.ombrachat to javafx.fxml;
    opens net.zonia3000.ombrachat.login to javafx.fxml;
    opens net.zonia3000.ombrachat.components.chat to javafx.fxml;
}
