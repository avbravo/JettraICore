#!/bin/bash

echo "===== JettraICore Builder & Runner ====="
echo "Versión: Java 25"

# 1. Compilación
echo "[1/2] Compilando el proyecto multi-módulo..."
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo "ERROR: La compilación falló. Revisa las dependencias (JitPack)."
    exit 1
fi

# 2. Ejecución
echo "[2/2] Iniciando Jettra 3D Native (Java)..."
mvn exec:java -pl jettra-3d-java
