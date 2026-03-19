package io.jettra.wui.complex;
import io.jettra.wui.core.UIComponent;

public class Table extends UIComponent {
    public Table() {
        super("table");
        this.initialClasses = "j-component";
        this.setStyle("width", "100%");
        this.setStyle("border-collapse", "collapse");
    }
    
    public void addRow(UIComponent... cells) {
        UIComponent tr = new UIComponent("tr"){};
        tr.setStyle("border-bottom", "1px solid var(--jettra-border)");
        for (UIComponent cell : cells) {
            UIComponent td = new UIComponent("td"){};
            td.setStyle("padding", "10px");
            td.add(cell);
            tr.add(td);
        }
        add(tr);
    }
    
    public void addHeaderRow(String... headers) {
        UIComponent tr = new UIComponent("tr"){};
        tr.setStyle("border-bottom", "2px solid var(--jettra-accent)");
        for (String header : headers) {
            UIComponent th = new UIComponent("th"){};
            th.setContent(header);
            th.setStyle("padding", "12px");
            th.setStyle("text-align", "left");
            th.setStyle("color", "var(--jettra-accent)");
            tr.add(th);
        }
        add(tr);
    }
}
