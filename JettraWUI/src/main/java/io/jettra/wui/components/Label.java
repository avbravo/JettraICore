package io.jettra.wui.components;
import io.jettra.wui.core.UIComponent;
public class Label extends UIComponent {
    public Label(String forId, String text) {
        super("label");
        if (forId != null && !forId.isEmpty()) {
            setProperty("for", forId);
        }
        setContent(text);
        this.setStyle("display", "block").setStyle("margin-bottom", "5px").setStyle("font-size", "0.9em");
    }
}
