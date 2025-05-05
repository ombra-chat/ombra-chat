package net.zonia3000.ombrachat.chat.message;

import org.drinkless.tdlib.TdApi;

public class MessageUtils {

    public static String getTextContent(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText messageText) {
            return messageText.text.text;
        }
        if (content instanceof TdApi.MessagePhoto messagePhoto) {
            if (messagePhoto.caption != null) {
                return messagePhoto.caption.text;
            }
            return null;
        }
        if (content instanceof TdApi.MessageDocument messageDocument) {
            if (messageDocument.caption != null) {
                return messageDocument.caption.text;
            }
            return null;
        }
        if (content instanceof TdApi.MessageVideo messageVideo) {
            if (messageVideo.caption != null) {
                return messageVideo.caption.text;
            }
            return null;
        }
        return null;
    }
}
