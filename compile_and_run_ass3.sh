#!/bin/bash

# --- Configuration ---

PROJECT_ROOT="$(pwd)"
ANALYTICS_DIR="${PROJECT_ROOT}/Assignments/Assignment3"
PYTHON_PATH="/c/Users/User/anaconda3/python.exe"
PY_SCRIPT="${ANALYTICS_DIR}/analytics.py"

# --- Defaults ---
DEFAULT_NUM_STATIONS=4
DEFAULT_P_START=1  # Using integers: 1 = 0.1, 9 = 0.9
DEFAULT_P_END=9
DEFAULT_P_STEP=1
DEFAULT_FRAMES_PER_STATION=50
DEFAULT_SLOT_TIME_MS=10
DEFAULT_SIM_SLOTS=1000
DEFAULT_BASE_PORT=5000

# --- User Inputs ---
read -p "Number of stations [${DEFAULT_NUM_STATIONS}]: " NUM_STATIONS
NUM_STATIONS=${NUM_STATIONS:-$DEFAULT_NUM_STATIONS}

echo "P-value range (using integers): 1=0.1, 2=0.2, ..., 9=0.9"
read -p "Starting p-value (1-9) [${DEFAULT_P_START}]: " P_START_INT
P_START_INT=${P_START_INT:-$DEFAULT_P_START}

read -p "Ending p-value (1-9) [${DEFAULT_P_END}]: " P_END_INT
P_END_INT=${P_END_INT:-$DEFAULT_P_END}

read -p "P-value step [${DEFAULT_P_STEP}]: " P_STEP_INT
P_STEP_INT=${P_STEP_INT:-$DEFAULT_P_STEP}

read -p "Frames per station [${DEFAULT_FRAMES_PER_STATION}]: " FRAMES_PER_STATION
FRAMES_PER_STATION=${FRAMES_PER_STATION:-$DEFAULT_FRAMES_PER_STATION}

read -p "Slot time (ms) [${DEFAULT_SLOT_TIME_MS}]: " SLOT_TIME_MS
SLOT_TIME_MS=${SLOT_TIME_MS:-$DEFAULT_SLOT_TIME_MS}

read -p "Total simulation slots [${DEFAULT_SIM_SLOTS}]: " SIM_SLOTS
SIM_SLOTS=${SIM_SLOTS:-$DEFAULT_SIM_SLOTS}

read -p "Base port [${DEFAULT_BASE_PORT}]: " BASE_PORT
BASE_PORT=${BASE_PORT:-$DEFAULT_BASE_PORT}

# --- Compile Java sources ---
echo "=== Compiling Java sources ==="
javac -d "${PROJECT_ROOT}/out" "${PROJECT_ROOT}/Assignments/Assignment3"/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed. Exiting."
    exit 1
fi

# --- Clean up existing CSV files ---
echo "=== Cleaning up existing CSV files ==="
#rm -f "${ANALYTICS_DIR}"/csma_cd_results_*.csv
#rm -f csma_cd_results_*.csv 2>/dev/null

# --- Run CSMA/CD Simulations ---
echo "=== Starting P-Persistent CSMA/CD Simulations ==="
echo "Configuration: ${NUM_STATIONS} stations, p-range ${P_START_INT}-${P_END_INT} (step ${P_STEP_INT})"
echo "Frames per station: ${FRAMES_PER_STATION}, Slot time: ${SLOT_TIME_MS}ms, Total slots: ${SIM_SLOTS}"
echo "Base port: ${BASE_PORT}"
echo

# Create array of p-values without bc (using integer arithmetic)
P_VALUES=()
for ((i=P_START_INT; i<=P_END_INT; i+=P_STEP_INT)); do
    P_VALUES+=($i)
done

# shellcheck disable=SC2145
echo "P-values to simulate (integers): ${P_VALUES[@]}"
echo

# Run simulation for each p-value
for p_int in "${P_VALUES[@]}"; do
    # Convert integer to decimal (1 -> 0.1, 5 -> 0.5, etc.)
    p_decimal="0.$p_int"

    # Calculate dynamic port to avoid conflicts (simple integer math)
    CURRENT_PORT=$((BASE_PORT + p_int))

    echo "--- Running simulation with p = $p_decimal on port ${CURRENT_PORT} ---"

    # Debug output
    echo "Command: java -cp ${PROJECT_ROOT}/out Assignments.Assignment3.Main ${NUM_STATIONS} $p_decimal ${FRAMES_PER_STATION} ${SLOT_TIME_MS} ${SIM_SLOTS} ${CURRENT_PORT}"

    # Run the simulation
    java -cp "${PROJECT_ROOT}/out" Assignments.Assignment3.Main ${NUM_STATIONS} $p_decimal ${FRAMES_PER_STATION} ${SLOT_TIME_MS} ${SIM_SLOTS} ${CURRENT_PORT}

    SIMULATION_EXIT_CODE=$?

    if [ $SIMULATION_EXIT_CODE -eq 0 ]; then
        echo "✓ Simulation with p = $p_decimal completed successfully"
    else
        echo "✗ Simulation with p = $p_decimal failed (exit code: $SIMULATION_EXIT_CODE)"
        echo "  Possible issues:"
        echo "  1. Main class not found in compiled output"
        echo "  2. MediumServer constructor doesn't accept p parameter"
        echo "  3. Java runtime error - check console output above"
    fi

    echo

    # Brief pause between simulations
    sleep 1
done

# Allow processing time
sleep 3

# --- Verify CSV files were generated ---
echo "=== Verifying CSV output files ==="

# Check in analytics directory first
CSV_COUNT=$(ls "${ANALYTICS_DIR}"/csma_cd_results_*.csv 2>/dev/null | wc -l)

if [ $CSV_COUNT -gt 0 ]; then
    echo "Generated ${CSV_COUNT} CSV files in analytics directory:"
    ls -la "${ANALYTICS_DIR}"/csma_cd_results_*.csv
    echo
elif [ -f "csma_cd_results_0.1.csv" ] || [ -f "csma_cd_results_0.5.csv" ]; then
    echo "Found CSV files in current directory:"
    ls -la csma_cd_results_*.csv 2>/dev/null
    echo "Moving to analytics directory..."
    mkdir -p "${ANALYTICS_DIR}"
    mv csma_cd_results_*.csv "${ANALYTICS_DIR}/" 2>/dev/null
    echo "CSV files moved successfully."
else
    echo "WARNING: No CSV files were generated!"
    echo
    echo "Troubleshooting steps:"
    echo "1. Check if Java compilation was successful above"
    echo "2. Verify MediumServer.java has been modified to accept p parameter"
    echo "3. Check if MetricsCollector.exportCsv() method exists"
    echo "4. Try running a single simulation manually:"
    echo "   java -cp ${PROJECT_ROOT}/out Assignments.Assignment3.Main 4 0.5 50 10 1000 5005"
    echo
    exit 1
fi

# --- Run Python analytics ---
sleep 2

if [ -f "$PY_SCRIPT" ]; then
    echo "=== Running Python analytics ==="

    cd "${ANALYTICS_DIR}"

    # Try multiple Python commands
    if "$PYTHON_PATH" "$(basename "$PY_SCRIPT")" 2>/dev/null; then
        echo "Python analysis completed successfully with configured Python path"
    elif python3 "$(basename "$PY_SCRIPT")" 2>/dev/null; then
        echo "Python analysis completed successfully with python3"
    elif python "$(basename "$PY_SCRIPT")" 2>/dev/null; then
        echo "Python analysis completed successfully with python"
    else
        echo "Python analysis failed with all attempted Python commands"
        echo "Try running manually: cd ${ANALYTICS_DIR} && python $(basename "$PY_SCRIPT")"
    fi

    echo "=== Analysis complete ==="

    # List generated plot files
    if [ -f "throughput_vs_p.png" ] || [ -f "delay_vs_p.png" ]; then
        echo "Generated plot files:"
        ls -la *.png 2>/dev/null
    else
        echo "No plot files generated - check Python script output above"
    fi

else
    echo "No Python analytics script found at $PY_SCRIPT"
    echo "Looking for plot_csma_cd.py..."
    if [ -f "${ANALYTICS_DIR}/plot_csma_cd.py" ]; then
        echo "Found plot_csma_cd.py, updating script path..."
        PY_SCRIPT="${ANALYTICS_DIR}/plot_csma_cd.py"
        cd "${ANALYTICS_DIR}"
        python3 "plot_csma_cd.py" 2>/dev/null || python "plot_csma_cd.py" 2>/dev/null || echo "Plot generation failed"
    else
        echo "No plotting script found - skipping plot generation"
    fi
fi

echo
echo "=== P-Persistent CSMA/CD Simulation Suite Complete ==="

# Final verification
CSV_COUNT_FINAL=$(ls "${ANALYTICS_DIR}"/csma_cd_results_*.csv 2>/dev/null | wc -l)
echo "Results: ${CSV_COUNT_FINAL} CSV files with performance metrics generated"

if [ -f "${ANALYTICS_DIR}/throughput_vs_p.png" ]; then
    echo "Plots: Performance graphs generated successfully"
else
    echo "Plots: Not generated - check Python environment and script"
fi

echo
echo "Files location: ${ANALYTICS_DIR}/"
echo "✓ Script execution completed"
