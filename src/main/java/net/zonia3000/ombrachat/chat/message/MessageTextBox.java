package net.zonia3000.ombrachat.chat.message;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.drinkless.tdlib.TdApi;

public class MessageTextBox extends VBox {

    public MessageTextBox(TdApi.MessageText messageText) {
        Label textLabel = new Label(messageText.text.text);
        textLabel.setWrapText(true);
        getChildren().add(textLabel);
    }
}
