package io.jettra.wui.components;
import io.jettra.wui.core.UIComponent;
public class Span extends UIComponent {
    public Span(String text) {
        super("span");
        setContent(text);
    }
}
