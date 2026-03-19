package io.jettra.wui.complex;
import io.jettra.wui.components.Div;

public class Grid extends Div {
    public Grid(int columns, String gap) {
        super();
        this.setStyle("display", "grid");
        this.setStyle("grid-template-columns", "repeat(" + columns + ", 1fr)");
        this.setStyle("gap", gap);
        this.initialClasses = "j-grid";
    }
}
