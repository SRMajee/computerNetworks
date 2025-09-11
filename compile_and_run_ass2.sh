#!/bin/bash

# --- Configuration ---
PROJECT_ROOT="$(pwd)"
ANALYTICS_DIR="${PROJECT_ROOT}/Assignments/Assignment2"
PYTHON_PATH="/c/Users/User/anaconda3/python.exe"
PY_SCRIPT="${ANALYTICS_DIR}/analytics.py"

# --- Defaults ---
DEFAULT_PORT=5000
DEFAULT_INPUT="Assignments/Assignment2/inputfile.txt"
DEFAULT_SENDER_MAC="98-BA-5F-ED-66-B7"
DEFAULT_RECEIVER_MAC="AA-BA-5F-ED-66-B7"

# --- User Inputs ---
read -p "Port [${DEFAULT_PORT}]: " PORT
PORT=${PORT:-$DEFAULT_PORT}

read -p "Input file path [${DEFAULT_INPUT}]: " INPUTFILE
INPUTFILE=${INPUTFILE:-$DEFAULT_INPUT}

read -p "Sender MAC [${DEFAULT_SENDER_MAC}]: " SENDER_MAC
SENDER_MAC=${SENDER_MAC:-$DEFAULT_SENDER_MAC}

read -p "Receiver MAC [${DEFAULT_RECEIVER_MAC}]: " RECEIVER_MAC
RECEIVER_MAC=${RECEIVER_MAC:-$DEFAULT_RECEIVER_MAC}

# --- Compile Java sources ---
echo "=== Compiling Java sources ==="
javac "${PROJECT_ROOT}/Assignments/Assignment2"/*.java
if [ $? -ne 0 ]; then
  echo "Compilation failed. Exiting."
  exit 1
fi

# --- Run Receiver ---
echo "=== Starting Receiver on port ${PORT} ==="
java -cp "${PROJECT_ROOT}" Assignments.Assignment2.Receiver "${PORT}" &
RECEIVER_PID=$!

# Allow receiver to start
sleep 1

# --- Run Sender ---
echo "=== Starting Sender ==="
java -cp "${PROJECT_ROOT}" Assignments.Assignment2.Sender localhost "${PORT}" "${INPUTFILE}" "${SENDER_MAC}" "${RECEIVER_MAC}"

# Allow processing time
sleep 2

# Terminate receiver (ignore error if already exited)
echo "Terminating Receiver (PID ${RECEIVER_PID})"
kill "${RECEIVER_PID}" 2>/dev/null || true

# --- Run Python analytics ---
if [ -f "$PY_SCRIPT" ]; then
    echo "=== Running Python analytics ==="
    "$PYTHON_PATH" "$PY_SCRIPT"
    if [ $? -ne 0 ]; then
        echo "Python analysis failed."
        exit 1
    fi
    echo "=== Analysis complete: detection_histogram.png and performance_metrics.csv generated ==="
else
    echo "No Python analytics script found at $PY_SCRIPT — skipping analysis"
fi
#echo "=== Analysis complete: detection_histogram.png and performance_metrics.csv generated ==="
