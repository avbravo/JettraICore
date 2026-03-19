# JettraWUI Examples
To build a fully functional futuristic 3D Web UI from Java 25, simply assemble the components as follows:

```java
import io.jettra.wui.core.Page;
import io.jettra.wui.components.Form;
import io.jettra.wui.components.Button;
import io.jettra.wui.components.TextBox;

public class Main {
    public static void main(String[] args) {
        // 1. Initialize Root Page (adds <body> and CSS internally)
        Page page = new Page("Jettra Enclave 3D");

        // 2. Initialize Container
        Form form = new Form("login-form", "/api/auth");
        
        // 3. Add Controls
        TextBox username = new TextBox("text", "username");
        username.setPlaceholder("Agent Identifier...");
        
        TextBox password = new TextBox("password", "password");
        password.setPlaceholder("Passcode...");
        
        Button loginBtn = new Button("Unlock Terminal");
        loginBtn.setStyle("margin-top", "20px");

        // 4. Assemble Hierarchy
        form.add(username);
        form.add(password);
        form.add(loginBtn);
        
        page.add(form);

        // 5. Generate and Output HTML
        String rawHtml = page.render();
        System.out.println(rawHtml);
        
        // Output can then be written to index.html or served via REST Application.
    }
}
```
