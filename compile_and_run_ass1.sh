#!/usr/bin/env bash

# Defaults
DEFAULT_PORT=5000
DEFAULT_INPUT="Assignments/Assignment1/inputfile.txt"
DEFAULT_SENDER_MAC="98-BA-5F-ED-66-B7"
DEFAULT_RECEIVER_MAC="AA-BA-5F-ED-66-B7"

read -p "Port [${DEFAULT_PORT}]: " PORT
PORT=${PORT:-$DEFAULT_PORT}

read -p "Input file path [${DEFAULT_INPUT}]: " INPUTFILE
INPUTFILE=${INPUTFILE:-$DEFAULT_INPUT}

read -p "Sender MAC [${DEFAULT_SENDER_MAC}]: " SENDER_MAC
SENDER_MAC=${SENDER_MAC:-$DEFAULT_SENDER_MAC}

read -p "Receiver MAC [${DEFAULT_RECEIVER_MAC}]: " RECEIVER_MAC
RECEIVER_MAC=${RECEIVER_MAC:-$DEFAULT_RECEIVER_MAC}

echo "=== Compiling Java sources ==="
javac Assignments/Assignment1/*.java
if [ $? -ne 0 ]; then
  echo "Compilation failed. Exiting."
  exit 1
fi

echo "=== Starting Receiver on port ${PORT} ==="
java -cp . Assignments.Assignment1.Receiver "${PORT}" &
RECEIVER_PID=$!

sleep 1

echo "=== Starting Sender ==="
java -cp . Assignments.Assignment1.Sender localhost "${PORT}" "${INPUTFILE}" "${SENDER_MAC}" "${RECEIVER_MAC}"

echo
read -p "Press Enter to terminate Receiver..." dummy

echo "Terminating Receiver (PID ${RECEIVER_PID})"
kill "${RECEIVER_PID}"


echo "Done."
