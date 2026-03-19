package io.jettra.wui.complex;
import io.jettra.wui.core.UIComponent;

public class Image extends UIComponent {
    public Image(String src, String alt) {
        super("img");
        setProperty("src", src);
        setProperty("alt", alt);
        this.setStyle("max-width", "100%");
        this.setStyle("border-radius", "8px");
        this.setStyle("box-shadow", "0 4px 15px rgba(0, 0, 0, 0.5)");
    }
}
