package io.jettra.wui.complex;
import io.jettra.wui.core.UIComponent;

public class Carousel extends UIComponent {
    public Carousel(String id) {
        super("div");
        setProperty("id", id);
        this.initialClasses = "j-carousel j-component";
        this.setStyle("display", "flex");
        this.setStyle("overflow-x", "auto");
        this.setStyle("scroll-snap-type", "x mandatory");
        this.setStyle("gap", "15px");
        this.setStyle("padding", "10px");
        
        // Hide scrollbar syntax for cleaner UI
        this.setStyle("-ms-overflow-style", "none");
        this.setStyle("scrollbar-width", "none");
    }
    
    public void addSlide(UIComponent content) {
        UIComponent slide = new UIComponent("div"){};
        slide.setStyle("scroll-snap-align", "start");
        slide.setStyle("flex", "0 0 100%");
        slide.setStyle("min-width", "100%");
        slide.add(content);
        add(slide);
    }
}
