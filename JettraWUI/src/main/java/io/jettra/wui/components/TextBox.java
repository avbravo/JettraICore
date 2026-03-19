package io.jettra.wui.components;
import io.jettra.wui.core.UIComponent;
public class TextBox extends UIComponent {
    public TextBox(String type, String name) {
        super("input");
        setProperty("type", type);
        setProperty("name", name);
        this.initialClasses = "j-input";
    }
    public TextBox setPlaceholder(String placeholder) {
        setProperty("placeholder", placeholder);
        return this;
    }
    public TextBox setValue(String value) {
        setProperty("value", value);
        return this;
    }
}
