# Arquitectura de Jettra 3D Core (Java)

Este documento describe la arquitectura, dependencias y metodologías utilizadas en el proyecto Jettra 3D Java Core.

## 1. Arquitectura del Sistema

El proyecto está diseñado como un sistema de visualización 3D nativo y autónomo que interactúa con el ecosistema Jettra. Sigue un patrón de **Reactor Multi-módulo de Maven**.

### Módulos Principales:
*   **`jettra-3d-java`**: Contiene la lógica del motor de visualización, gestión de cámara, interfaz de usuario (RayGUI style) y el bucle principal del juego.
*   **`jettra-dl`**: Capa de datos y modelos compartidos. Define las entidades como `HumanEntity`, `Artifact`, y `WorldEvent`, permitiendo la interoperabilidad y serialización (JSON).

### Patrón de Ejecución (Game Loop):
La aplicación sigue el paradigma clásico de motores de videojuegos:
1.  **Input**: Captura de teclado, ratón y eventos del sistema.
2.  **Update**: Actualización de la posición de agentes, lógica de cámara y temporizadores de pensamientos.
3.  **Draw**: Renderizado de la escena 3D seguido de la capa de interfaz de usuario 2D (Overlay).

## 2. Dependencias y Librerías

El núcleo tecnológico se basa en el acceso nativo de alto rendimiento:

*   **Jaylib-FFM (Raylib)**: Se utiliza `io.github.electronstudio:jaylib-ffm`. Es una implementación de **Raylib 5.5** para Java utilizando la nueva **API Foreign Function & Memory (Project Panama)** de Java 25. Esto elimina la necesidad de JNI y proporciona acceso directo a la memoria de la GPU.
*   **Jackson Databind**: Para la comunicación y persistencia de estados de los agentes y el mundo en formato JSON.
*   **Java 25 (Native)**: El proyecto está configurado para ejecutarse en las versiones más recientes de Java para aprovechar las optimizaciones de memoria nativa.

## 3. Metodología de Programación

La programación en Jettra 3D Core sigue estándares modernos y reactivos:

### Estilo de Código:
*   **Native-First**: Uso intensivo de tipos de datos de Raylib (`Vector3`, `Color`, `Rectangle`) para minimizar la sobrecarga en el renderizado.
*   **Thread Safety**: Se utilizan colecciones concurrentes como `CopyOnWriteArrayList` para permitir que el motor de renderizado lea los datos mientras otros procesos (como la actualización de agentes) los modifican.
*   **Naming Convention**: Siguiendo la filosofía de `jaylib-ffm`, los métodos de Raylib se usan en estilo **lowerCamelCase** (ej. `initWindow()`, `drawCube()`), manteniendo la consistencia con las convenciones estándar de Java.

### Características Especiales:
*   **Cámara Dinámica**: Soporta modos de seguimiento de agentes, modo director automático y anclaje al plano.
*   **Sistema de Pensamientos**: Visualización efímera de estados internos mediante burbujas de diálogo con temporizadores integrados.
*   **UI Inmediata**: La interfaz de usuario se dibuja en cada frame de forma declarativa, permitiendo una interactividad fluida y un consumo de recursos predecible.

---
*Jettra Team - "Simulando el futuro con Java 25 Native"*
