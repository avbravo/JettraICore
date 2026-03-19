# JettraWUI Architecture
JettraWUI is a Java-to-HTML Component framework focused on generating futuristic, 3D visually-rich User Interfaces using Neumorphism and Glassmorphism CSS directly embedded into Java object hierarchies.

## Core Hierarchy
The framework is based on the Abstract Syntax Tree pattern:
- `UIComponent` es la clase base. Almacena las `properties` (HTML attributes), `styles` (inline CSS), e hijos `children` (otros componentes anidados).
- Cada componente específico (`Button`, `Form`, `Modal`) anula el método `render()` par construir la etiqueta HTML correspondiente y le adjunta sus sub-nodos llamando internamente a `render()` en sus clases hijas.
- `Page` es la raíz del árbol. Se encarga de rodear todos los elementos con las etiquetas `<html>`, `<head>` y `<body>`. Además, inyecta la librería estática de JettraTheme para dar vida al mundo 3D.

## Advantages
- **Zero Front-end Tech Required**: The user never touches CSS, JS, or HTML formats.
- **Reusable Types**: Objects like Forms or Grids can be initialized and reused programmatically.
- **Consistent Futuristic Concept**: `JettraTheme` enforces a strict aesthetic pattern focused on spatial depth, glass blur effects, and animations to resemble immersive environments.
