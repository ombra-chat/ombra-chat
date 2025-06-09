package net.zonia3000.ombrachat;

import java.lang.reflect.Field;
import net.zonia3000.ombrachat.controllers.ChatPageController;

public class TestUtils {

    public static <T, U> void setPrivateField(T target, String fieldName, U fieldValue) {
        try {
            Field field = ChatPageController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, fieldValue);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        }
    }
}
