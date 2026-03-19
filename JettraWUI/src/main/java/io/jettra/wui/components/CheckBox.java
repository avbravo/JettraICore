package io.jettra.wui.components;
import io.jettra.wui.core.UIComponent;

public class CheckBox extends UIComponent {
    public CheckBox(String id, String name, String value) {
        super("input");
        setProperty("type", "checkbox");
        setProperty("id", id);
        setProperty("name", name);
        setProperty("value", value);
        this.setStyle("margin-right", "8px");
        this.setStyle("accent-color", "var(--jettra-accent)");
    }
}
