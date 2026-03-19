package io.jettra.wui.components;
import io.jettra.wui.core.UIComponent;
public class Form extends UIComponent {
    public Form(String id, String action) {
        super("form");
        setProperty("id", id);
        setProperty("action", action);
        setProperty("method", "POST");
        this.initialClasses = "j-component";
    }
}
