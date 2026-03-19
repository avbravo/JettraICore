package io.jettra.wui.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class UIComponent {
    protected String tag;
    protected String content = "";
    protected List<UIComponent> children = new ArrayList<>();
    protected Map<String, String> properties = new LinkedHashMap<>();
    protected Map<String, String> styles = new LinkedHashMap<>();
    protected String initialClasses = "";

    public UIComponent(String tag) {
        this.tag = tag;
    }

    public UIComponent setProperty(String key, String value) {
        properties.put(key, value);
        return this;
    }

    public UIComponent setStyle(String key, String value) {
        styles.put(key, value);
        return this;
    }

    public UIComponent addClass(String className) {
        if (properties.containsKey("class")) {
            properties.put("class", properties.get("class") + " " + className);
        } else {
            properties.put("class", initialClasses + " " + className);
        }
        return this;
    }

    public UIComponent add(UIComponent child) {
        children.add(child);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String render() {
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(tag);
        
        // Ensure class property carries default styling logic
        if (!initialClasses.isEmpty() && !properties.containsKey("class")) {
            properties.put("class", initialClasses.trim());
        }

        // Properties
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            builder.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }
        
        // Styles
        if (!styles.isEmpty()) {
            builder.append(" style=\"");
            for (Map.Entry<String, String> entry : styles.entrySet()) {
                builder.append(entry.getKey()).append(":").append(entry.getValue()).append("; ");
            }
            builder.append("\"");
        }
        
        builder.append(">");
        builder.append(content);
        
        for (UIComponent child : children) {
            builder.append(child.render());
        }
        
        builder.append("</").append(tag).append(">\n");
        return builder.toString();
    }
}
