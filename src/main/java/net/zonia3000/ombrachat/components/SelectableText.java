package net.zonia3000.ombrachat.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.services.GuiService;

public class SelectableText extends TextFlow {

    private final StringProperty text = new SimpleStringProperty(this, "text", "");

    private boolean isSelecting = false;

    private final Text textNode;

    private int startIndex = 0;

    public SelectableText() {
        textNode = new Text();
        getChildren().add(textNode);

        textNode.setSelectionFill(Color.DEEPSKYBLUE);

        var guiService = ServiceLocator.getService(GuiService.class);

        setOnMousePressed(e -> {
            isSelecting = true;
            startIndex = getCharHitIndex(e);
            guiService.setCurrentSelectable(this);
            e.consume();
        });
        setOnMouseReleased(e -> {
            isSelecting = false;
            if (startIndex == getCharHitIndex(e)) {
                resetSelection();
            }
        });
        setOnMouseDragged(e -> {
            if (isSelecting) {
                var index = getCharHitIndex(e);
                if (index > startIndex) {
                    textNode.setSelectionStart(startIndex);
                    textNode.setSelectionEnd(index);
                } else {
                    textNode.setSelectionStart(index);
                    textNode.setSelectionEnd(startIndex);
                }
            }
        });
    }

    public String getText() {
        return text.get();
    }

    public void setText(String value) {
        text.setValue(value);
        textNode.setText(value);
    }

    public void resetSelection() {
        isSelecting = false;
        textNode.setSelectionStart(0);
        textNode.setSelectionEnd(0);
    }

    public String getSelectedText() {
        var textContent = textNode.getText();
        var start = textNode.getSelectionStart();
        if (start < 0) {
            start = 0;
        }
        var end = textNode.getSelectionEnd();
        if (end >= textContent.length()) {
            end = textContent.length() - 1;
        }
        if (start < 0 || end < 0 || end >= textContent.length()) {
            return "";
        }
        return textContent.substring(start, end);
    }

    private int getCharHitIndex(MouseEvent e) {
        var hit = textNode.hitTest(new Point2D(e.getX(), e.getY()));
        return hit.getCharIndex();
    }
}
