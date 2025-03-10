package net.zonia3000.ombrachat.chat.message;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.drinkless.tdlib.TdApi;

public class MessageNotSupportedBox extends VBox {

    public MessageNotSupportedBox(TdApi.MessageContent message) {
        Label textLabel = new Label("Not supported yet");
        textLabel.setWrapText(true);
        getChildren().add(textLabel);
    }
}
