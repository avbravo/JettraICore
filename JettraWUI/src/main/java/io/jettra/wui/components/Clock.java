package io.jettra.wui.components;
import io.jettra.wui.core.UIComponent;

public class Clock extends UIComponent {
    public Clock(String id) {
        super("div");
        setProperty("id", id);
        this.initialClasses = "j-clock j-component";
        this.setStyle("font-family", "monospace");
        this.setStyle("font-size", "2.5rem");
        this.setStyle("text-align", "center");
        this.setStyle("color", "var(--jettra-accent)");
        this.setStyle("text-shadow", "0 0 15px var(--jettra-glow)");
        
        UIComponent script = new UIComponent("script"){};
        script.setContent("setInterval(() => { const el = document.getElementById('"+id+"'); if(el) el.innerText = new Date().toLocaleTimeString(); }, 1000);");
        add(script);
    }
}
