package io.jettra.wui.assets;

public class JettraTheme {
    public static String getCSS() {
        return "<style>\n" +
            ":root {\n" +
            "  --jettra-bg: #0a0a0f;\n" +
            "  --jettra-glow: rgba(0, 240, 255, 0.4);\n" +
            "  --jettra-glass: rgba(20, 25, 40, 0.6);\n" +
            "  --jettra-border: rgba(100, 200, 255, 0.2);\n" +
            "  --jettra-text: #e0f0ff;\n" +
            "  --jettra-accent: #00e5ff;\n" +
            "}\n" +
            "body {\n" +
            "  background-color: var(--jettra-bg);\n" +
            "  background-image: radial-gradient(circle at 50% 50%, #151525 0%, #0a0a0f 100%);\n" +
            "  color: var(--jettra-text);\n" +
            "  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
            "  margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;\n" +
            "}\n" +
            ".jettra-viewport {\n" +
            "  width: 100%; max-width: 1200px; padding: 2rem; box-sizing: border-box;\n" +
            "  perspective: 1000px;\n" +
            "}\n" +
            ".j-component {\n" +
            "  background: var(--jettra-glass);\n" +
            "  border: 1px solid var(--jettra-border);\n" +
            "  border-radius: 12px;\n" +
            "  padding: 15px;\n" +
            "  backdrop-filter: blur(10px);\n" +
            "  box-shadow: 0 8px 32px 0 rgba(0,0,0,0.37), inset 0 0 10px rgba(0,255,255,0.05);\n" +
            "  transition: all 0.3s ease;\n" +
            "  margin-bottom: 15px;\n" +
            "}\n" +
            ".j-btn {\n" +
            "  background: linear-gradient(135deg, rgba(0,229,255,0.2) 0%, rgba(0,100,255,0.2) 100%);\n" +
            "  color: var(--jettra-accent);\n" +
            "  border: 1px solid var(--jettra-accent);\n" +
            "  padding: 10px 20px; border-radius: 8px; cursor: pointer;\n" +
            "  text-transform: uppercase; font-weight: bold; letter-spacing: 1px;\n" +
            "  box-shadow: 0 0 10px var(--jettra-glow);\n" +
            "  transition: all 0.3s ease;\n" +
            "  width: 100%;\n" +
            "}\n" +
            ".j-btn:hover {\n" +
            "  background: var(--jettra-accent);\n" +
            "  color: #000; box-shadow: 0 0 20px var(--jettra-glow), 0 0 40px var(--jettra-accent);\n" +
            "  transform: translateZ(10px);\n" +
            "}\n" +
            ".j-input {\n" +
            "  background: rgba(0,0,0,0.5);\n" +
            "  border: 1px solid var(--jettra-border);\n" +
            "  color: var(--jettra-text);\n" +
            "  padding: 12px; border-radius: 6px; width: 100%; box-sizing: border-box;\n" +
            "  outline: none; margin-top: 5px; margin-bottom: 15px;\n" +
            "  transition: all 0.2s;\n" +
            "}\n" +
            ".j-input:focus {\n" +
            "  border-color: var(--jettra-accent);\n" +
            "  box-shadow: inset 0 0 8px var(--jettra-glow);\n" +
            "}\n" +
            "</style>\n";
    }
    
    public static String getJS() {
        return "<script>\n" +
            "document.addEventListener('mousemove', (e) => {\n" +
            "  const cards = document.querySelectorAll('.j-component');\n" +
            "  cards.forEach(card => {\n" +
            "    const rect = card.getBoundingClientRect();\n" +
            "    const x = e.clientX - rect.left; const y = e.clientY - rect.top;\n" +
            "    /* Optional subtle tracking effect can be added here */ \n" +
            "  });\n" +
            "});\n" +
            "</script>\n";
    }
}
