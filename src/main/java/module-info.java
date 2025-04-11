module ombrachat {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.drinkless.tdlib;
    requires java.prefs;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pg;
    requires org.slf4j;
    requires org.slf4j.simple;
    exports net.zonia3000.ombrachat;
    exports net.zonia3000.ombrachat.chat;
    opens net.zonia3000.ombrachat to javafx.fxml;
    opens net.zonia3000.ombrachat.chat to javafx.fxml;
    opens net.zonia3000.ombrachat.controllers to javafx.fxml;
}
