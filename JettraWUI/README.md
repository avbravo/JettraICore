# JettraWUI

JettraWUI (Web User Interface) es un framework avanzado en Java 25 para generar interfaces web utilizando la arquitectura de los Árboles de Análisis Sintáctico Abstractos (AST). Permite construir interfaces de estilo futurista (Neumorfismo, Glassmorfismo y entornos pseudotridimensionales) utilizando código puramente Java, sin requerir HTML, CSS, o dependencias estáticas de Javascript.

## Requisitos Previos

- **Java 25** o superior instalado y configurado en tus variables de entorno `$JAVA_HOME`.
- **Apache Maven** 3.9+ instalado y configurado en el `PATH`.
- Un entorno de Linux/Ubuntu o sistema compatible.

## Compilar y Generar el JAR

JettraWUI está configurado como un módulo dependiente dentro de la super-suite `JettraICore`. Para empaquetar JettraWUI en su propio archivo `.jar`, de modo que otros proyectos de tu ordenador puedan utilizarlo como librería:

1. **Abre tu terminal** y navega al directorio del núcleo (JettraICore):
   ```bash
   cd /home/avbravo/NetBeansProjects/jettrastack_local/JettraICore
   ```

2. **Ejecuta la limpieza y la instalación de Maven** focalizada en el proyecto JettraWUI:
   ```bash
   mvn clean install -pl JettraWUI -am
   ```
   *Nota: `-pl JettraWUI` significa que solo empaquetará la carpeta JettraWUI, y `-am` significa "also make" compilará cualquier dependencia padre necesaria (`JettraICore Parent`).*

3. **Verifica el JAR generado**:
   Tras finalizar exitosamente el proceso de Maven, puedes encontrar el archivo compilado listo para usar en:
   ```txt
   JettraWUI/target/JettraWUI-1.0.0-SNAPSHOT.jar
   ```

   Maven también lo instalará de forma automática en tu repositorio `~/.m2/repository/io/jettra/wui/JettraWUI/`, por lo que ya podrás importarlo en cualquier otro archivo `pom.xml` de otros proyectos utilizando la dependencia:
   ```xml
   <dependency>
       <groupId>io.jettra.wui</groupId>
       <artifactId>JettraWUI</artifactId>
       <version>1.0.0-SNAPSHOT</version>
   </dependency>
   ```

## Documentación y Ejemplos

En el directorio `/guide/` dentro de este proyecto encontrarás:
- [arquitectura.md](./guide/arquitectura.md): Explicación técnica detallada del funcionamiento del AST de inyección DOM.
- [example.md](./guide/example.md): Fragmentos de código en Java mostrando cómo iniciar el `Page`, envolver elementos en el formulario de acceso `Login`, e imprimir la cadena final en `<HTML>`.
