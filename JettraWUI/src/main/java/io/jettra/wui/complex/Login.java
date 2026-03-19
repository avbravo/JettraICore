package io.jettra.wui.complex;
import io.jettra.wui.components.*;

public class Login extends Form {
    public Login(String action) {
        super("login-form", action);
        this.setStyle("max-width", "400px");
        this.setStyle("margin", "0 auto");
        
        Header header = new Header(2, "Secure Access");
        header.setStyle("text-align", "center");
        add(header);
        
        Label userLabel = new Label("username", "Identification Code");
        TextBox userField = new TextBox("text", "username");
        userField.setProperty("required", "true");
        add(userLabel).add(userField);
        
        Label passLabel = new Label("password", "Authorization Key");
        TextBox passField = new TextBox("password", "password");
        passField.setProperty("required", "true");
        add(passLabel).add(passField);
        
        Button submit = new Button("AUTHENTICATE");
        submit.setStyle("margin-top", "20px");
        add(submit);
    }
}
