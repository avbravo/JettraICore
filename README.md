# JettraICore

Proyecto núcleo de Jettra desarrollado en **Java 25** con arquitectura multi-módulo Maven.

## Estructura del Proyecto

*   **jettra-3d-java**: Implementación de la interfaz 3D nativa utilizando Raylib (vía `jaylib-ffm`). Replicación idéntica de la versión en Go.
*   **jettra-dl**: Componentes de IA y Deep Learning para la autonomía de los agentes.

## Requisitos

*   **Java 25** o superior.
*   **Maven 3.9+**.
*   Librerías de desarrollo de Raylib/X11 instaladas en el sistema (ver script `install_deps.sh` en la carpeta original de Go).

## Instrucciones de Compilación

Desde la raíz del proyecto `JettraICore`, ejecuta:

```bash
mvn clean install
```

## Instrucciones de Ejecución

Para iniciar la simulación 3D en Java:

```bash
mvn exec:java -pl jettra-3d-java
```

> **Nota**: El proyecto utiliza el FFM API (Project Panama) de Java 25 para el acceso nativo a Raylib. Los parámetros `--enable-native-access=ALL-UNNAMED` y `--enable-preview` están configurados automáticamente en el `pom.xml`.

## Autores
Desarrollado como parte del ecosistema Jettra.
