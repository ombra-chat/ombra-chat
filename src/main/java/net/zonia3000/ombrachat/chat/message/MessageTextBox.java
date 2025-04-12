package net.zonia3000.ombrachat.chat.message;

import javafx.scene.layout.VBox;
import net.zonia3000.ombrachat.components.SelectableText;
import org.drinkless.tdlib.TdApi;

public class MessageTextBox extends VBox {

    private final SelectableText selectableText;

    public MessageTextBox(TdApi.MessageText messageText) {
        selectableText = new SelectableText();
        selectableText.setText(messageText.text.text);
        getChildren().add(selectableText);
    }
}
