package io.jettra.wui.components;
import io.jettra.wui.core.UIComponent;
public class Button extends UIComponent {
    public Button(String text) {
        super("button");
        setContent(text);
        this.initialClasses = "j-btn";
    }
}
