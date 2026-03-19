package io.jettra.wui.complex;
import io.jettra.wui.core.UIComponent;

public class Menu extends UIComponent {
    public Menu() {
        super("nav");
        this.initialClasses = "j-menu j-component";
        this.setStyle("display", "flex");
        this.setStyle("gap", "20px");
        this.setStyle("padding", "15px 20px");
        this.setStyle("border-radius", "8px");
    }

    public void addItem(String label, String link) {
        UIComponent a = new UIComponent("a"){};
        a.setContent(label);
        a.setProperty("href", link);
        a.setStyle("color", "var(--jettra-text)");
        a.setStyle("text-decoration", "none");
        a.setStyle("font-weight", "bold");
        a.setStyle("transition", "color 0.3s, text-shadow 0.3s");
        a.setProperty("onmouseover", "this.style.color='var(--jettra-accent)'; this.style.textShadow='0 0 10px var(--jettra-glow)'");
        a.setProperty("onmouseout", "this.style.color='var(--jettra-text)'; this.style.textShadow='none'");
        add(a);
    }
}
