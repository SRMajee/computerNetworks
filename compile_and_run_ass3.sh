#!/bin/bash

# --- Configuration ---
PROJECT_ROOT="$(pwd)"
ANALYTICS_DIR="${PROJECT_ROOT}/Assignments/Assignment3"
PYTHON_PATH="/c/Users/User/anaconda3/python.exe"
PY_SCRIPT="${ANALYTICS_DIR}/analytics.py"

# --- Defaults ---
DEFAULT_PORT=5000
DEFAULT_INPUT="Assignments/Assignment3/inputfile.txt"
DEFAULT_SENDER_MAC="98-BA-5F-ED-66-B7"
DEFAULT_RECEIVER_MAC="AA-BA-5F-ED-66-B7"
DEFAULT_P_VALUES="0.1,0.3,0.5,0.7,0.9"

# --- User Inputs ---
echo "=== p-persistent CSMA-CD Simulation Configuration ==="
read -p "Port [${DEFAULT_PORT}]: " PORT
PORT=${PORT:-$DEFAULT_PORT}

read -p "Input file path [${DEFAULT_INPUT}]: " INPUTFILE
INPUTFILE=${INPUTFILE:-$DEFAULT_INPUT}

read -p "Sender MAC [${DEFAULT_SENDER_MAC}]: " SENDER_MAC
SENDER_MAC=${SENDER_MAC:-$DEFAULT_SENDER_MAC}

read -p "Receiver MAC [${DEFAULT_RECEIVER_MAC}]: " RECEIVER_MAC
RECEIVER_MAC=${RECEIVER_MAC:-$DEFAULT_RECEIVER_MAC}

read -p "p values to test (comma-separated) [${DEFAULT_P_VALUES}]: " P_VALUES
P_VALUES=${P_VALUES:-$DEFAULT_P_VALUES}

# --- Compile Java sources ---
echo "=== Compiling Java sources ==="
javac -d "${PROJECT_ROOT}/out" "${PROJECT_ROOT}/Assignments/Assignment3"/*.java
if [ $? -ne 0 ]; then
  echo "Compilation failed. Exiting."
  exit 1
fi

# --- Run Receiver ---
echo "=== Starting Receiver on port ${PORT} ==="
java -cp "${PROJECT_ROOT}/out" Assignments.Assignment3.Receiver "${PORT}" &
RECEIVER_PID=$!

# Allow receiver to start
sleep 2

echo "Receiver started with PID: ${RECEIVER_PID}"

# --- Run Sender for each p value ---
echo "=== Running CSMA-CD Simulations ==="
echo "Configuration:"
echo "  - Host: localhost"
echo "  - Port: ${PORT}"
echo "  - Input file: ${INPUTFILE}"
echo "  - Sender MAC: ${SENDER_MAC}"
echo "  - Receiver MAC: ${RECEIVER_MAC}"
echo "  - P values: ${P_VALUES}"
echo ""

# Convert comma-separated p values to array
IFS=',' read -ra P_ARRAY <<< "$P_VALUES"

# Run simulation for each p value automatically
for p in "${P_ARRAY[@]}"; do
    p=$(echo "$p" | xargs) # Trim whitespace
    echo "=== Running CSMA-CD simulation with p = ${p} ==="

    # Create a temporary script to automate sender input
    temp_script=$(mktemp)

    # Determine menu choice based on p value
    case "$p" in
        "0.1") choice=1 ;;
        "0.3") choice=2 ;;
        "0.5") choice=3 ;;
        "0.7") choice=4 ;;
        "0.9") choice=5 ;;
        *) choice=1 ;; # Default to 0.1
    esac

    # Write choices to temp script: choice, then exit (0)
    echo -e "${choice}\n0" > "$temp_script"

    # Run sender with automated input
    java -cp "${PROJECT_ROOT}" Assignments.Assignment3.Sender localhost "${PORT}" "${INPUTFILE}" "${SENDER_MAC}" "${RECEIVER_MAC}" < "$temp_script"

    rm "$temp_script"

    if [ $? -ne 0 ]; then
        echo "Sender execution failed for p = ${p}"
    else
        echo "Simulation complete for p = ${p}"
    fi

    echo "Waiting 2 seconds before next simulation..."
    sleep 2
    echo ""
done

# Allow final processing time
sleep 2

# Terminate receiver (ignore error if already exited)
echo "Terminating Receiver (PID ${RECEIVER_PID})"
kill "${RECEIVER_PID}" 2>/dev/null || true

# Wait a moment for cleanup
sleep 1

# --- Combine results into summary CSV ---
echo "=== Generating combined results ==="
SUMMARY_CSV="${ANALYTICS_DIR}/combined_results.csv"
echo "p,Throughput,Average_Delay,Efficiency,Collision_Rate,Total_Transmitted,Total_Received,Total_Collisions" > "$SUMMARY_CSV"

for p in "${P_ARRAY[@]}"; do
    p=$(echo "$p" | xargs)
    p_filename=$(echo "$p" | sed 's/\.//g')
    INDIVIDUAL_CSV="${ANALYTICS_DIR}/performance_metrics_p${p_filename}.csv"

    if [ -f "$INDIVIDUAL_CSV" ]; then
        # Extract values from individual CSV
        throughput=$(grep "Throughput," "$INDIVIDUAL_CSV" | cut -d',' -f2)
        delay=$(grep "Average_Forwarding_Delay," "$INDIVIDUAL_CSV" | cut -d',' -f2)
        efficiency=$(grep "Efficiency," "$INDIVIDUAL_CSV" | cut -d',' -f2)
        transmitted=$(grep "Total_Frames_Expected," "$INDIVIDUAL_CSV" | cut -d',' -f2)
        received=$(grep "Total_Frames_Received," "$INDIVIDUAL_CSV" | cut -d',' -f2)

        # Calculate collision rate and other metrics
        if [ -n "$transmitted" ] && [ "$transmitted" != "0" ]; then
            collision_rate=$(echo "scale=4; (($transmitted - $received) / $transmitted)" | bc -l 2>/dev/null || echo "0.0000")
            collisions=$(echo "($transmitted - $received)" | bc -l 2>/dev/null || echo "0")
        else
            collision_rate="0.0000"
            collisions="0"
        fi

        # Add to summary CSV
        echo "${p},${throughput},${delay},${efficiency},${collision_rate},${transmitted},${received},${collisions}" >> "$SUMMARY_CSV"
        echo "Processed results for p = ${p}"
    else
        echo "Warning: Results file not found for p = ${p} (${INDIVIDUAL_CSV})"
    fi
done

echo "Combined results saved to: ${SUMMARY_CSV}"

# --- Run Python analytics ---
if [ -f "$PY_SCRIPT" ]; then
    echo "=== Running Python analytics ==="
    "$PYTHON_PATH" "$PY_SCRIPT"
    if [ $? -ne 0 ]; then
        echo "Python analysis failed, but continuing..."
    else
        echo "=== Analysis complete: performance_comparison.png and throughput_delay_analysis.png generated ==="
    fi
else
    echo "No Python analytics script found at $PY_SCRIPT — skipping analysis"
fi

echo ""
echo "=== p-persistent CSMA-CD Simulation Complete ==="
echo "Results available in: ${ANALYTICS_DIR}/"
echo "  - Individual CSV files: performance_metrics_p*.csv"
echo "  - Combined results: combined_results.csv"
echo "  - Performance graphs: *.png files (if Python analytics ran successfully)"
echo ""
echo "Command used:"
echo "  java Assignments.Assignment3.Sender localhost ${PORT} ${INPUTFILE} ${SENDER_MAC} ${RECEIVER_MAC}"
