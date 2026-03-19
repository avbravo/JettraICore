package io.jettra.wui.core;

import io.jettra.wui.assets.JettraTheme;

public class Page extends UIComponent {
    
    private String title;

    public Page(String title) {
        super("html");
        this.title = title;
    }

    @Override
    public String render() {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>\n");
        builder.append("<html lang=\"en\">\n<head>\n");
        builder.append("    <meta charset=\"UTF-8\">\n");
        builder.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        builder.append("    <title>").append(title).append("</title>\n");
        
        // Inject 3D Futuristic Theme CSS & JS
        builder.append(JettraTheme.getCSS());
        builder.append(JettraTheme.getJS());
        
        builder.append("</head>\n<body>\n");
        
        // Main container
        builder.append("<div class=\"jettra-viewport\">\n");
        
        for (UIComponent child : children) {
            builder.append(child.render());
        }
        
        builder.append("</div>\n");
        builder.append("</body>\n</html>");
        
        return builder.toString();
    }
}
