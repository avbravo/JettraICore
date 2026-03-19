package io.jettra.wui.complex;
import io.jettra.wui.core.UIComponent;

public class Modal extends UIComponent {
    public Modal(String id) {
        super("div");
        setProperty("id", id);
        this.initialClasses = "j-component";
        this.setStyle("display", "none");
        this.setStyle("position", "fixed");
        this.setStyle("top", "50%");
        this.setStyle("left", "50%");
        this.setStyle("transform", "translate(-50%, -50%)");
        this.setStyle("z-index", "1000");
        this.setStyle("min-width", "300px");
        this.setStyle("box-shadow", "0 0 50px rgba(0,255,255,0.2), 0 0 100px rgba(0,0,0,0.8)");
    }
}
