#!/bin/bash

# Compile all Java files
echo "Compiling Java files..."
javac -d . csma/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo "Running CSMA-CD Simulation..."
    echo "================================"
    java csma.CSMASimulation
else
    echo "Compilation failed!"
    exit 1
fi
