package io.jettra.wui.components;
import io.jettra.wui.core.UIComponent;
public class Header extends UIComponent {
    public Header(int level, String text) {
        super("h" + Math.max(1, Math.min(6, level)));
        setContent(text);
        this.initialClasses = "j-header";
        this.setStyle("color", "var(--jettra-accent)");
        this.setStyle("text-shadow", "0 0 10px var(--jettra-glow)");
    }
}
