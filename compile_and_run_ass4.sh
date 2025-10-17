#!/bin/bash

# --- Configuration ---
PROJECT_ROOT="$(pwd)"
ASSIGN_DIR="${PROJECT_ROOT}/Assignments/Assignment4"
OUT_DIR="${PROJECT_ROOT}/out"
PYTHON_PATH="/c/Users/User/anaconda3/python.exe"
PY_SCRIPT="${ASSIGN_DIR}/analytics.py"

# --- Defaults ---
DEFAULT_PORT=5000
DEFAULT_INPUT="${ASSIGN_DIR}/inputfile.txt"
DEFAULT_SENDER_MAC="98-BA-5F-ED-66-B7"
DEFAULT_RECEIVER_MAC="AA-BA-5F-ED-66-B7"

# --- User Inputs ---
read -p "Port [${DEFAULT_PORT}]: " PORT
PORT=${PORT:-$DEFAULT_PORT}

read -p "Input file path [${DEFAULT_INPUT}]: " INPUTFILE
INPUTFILE=${INPUTFILE:-$DEFAULT_INPUT}

read -p "Number of stations [8]: " NUM_STATIONS
NUM_STATIONS=${NUM_STATIONS:-8}

read -p "Sender MAC [${DEFAULT_SENDER_MAC}]: " SENDER_MAC
SENDER_MAC=${SENDER_MAC:-$DEFAULT_SENDER_MAC}

read -p "Receiver MAC [${DEFAULT_RECEIVER_MAC}]: " RECEIVER_MAC
RECEIVER_MAC=${RECEIVER_MAC:-$DEFAULT_RECEIVER_MAC}

# --- Compile ---
echo "=== Compiling Assignment 4 Java sources ==="
mkdir -p "${OUT_DIR}"
javac -d "${OUT_DIR}" "${ASSIGN_DIR}"/*.java || { echo "Compilation failed"; exit 1; }

# --- Run Receiver ---
echo "=== Starting Receiver on port ${PORT} ==="
java -cp "${OUT_DIR}" Assignments.Assignment4.Receiver "${PORT}" &
RECEIVER_PID=$!
sleep 1

# --- Run Sender ---
echo "=== Starting Sender with ${NUM_STATIONS} stations ==="
java -cp "${OUT_DIR}" Assignments.Assignment4.Sender localhost "${PORT}" "${INPUTFILE}" "${SENDER_MAC}" "${RECEIVER_MAC}" "${NUM_STATIONS}"

# --- Stop Receiver ---
sleep 2
echo "=== Terminating Receiver (PID ${RECEIVER_PID}) ==="
kill "${RECEIVER_PID}" 2>/dev/null || true

# --- Run Python analytics ---
if [ -f "$PY_SCRIPT" ]; then
    echo "=== Running Python analytics ==="
    "$PYTHON_PATH" "$PY_SCRIPT"
    echo "=== Analysis complete ==="
else
    echo "No analytics script found at $PY_SCRIPT — skipping"
fi
