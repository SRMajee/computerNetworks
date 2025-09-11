import pandas as pd
import matplotlib.pyplot as plt
import glob
import os
import numpy as np

# --- Config ---
base_path = "Assignments/Assignment2"
probs = [80, 85, 90, 95, 100]

schemes = {
    "StopAndWait": "csvframe_times_stop_and_wait{}.csv",
    "GoBackN": "csvframe_times_go_back_n{}.csv",
    "SelectiveRepeat": "csvframe_times_selective_repeat{}.csv"
}

# Store results: scheme → [avg time for each prob]
results = {scheme: [] for scheme in schemes}

# --- Compute averages ---
for prob in probs:
    for scheme, pattern in schemes.items():
        filename = os.path.join(base_path, pattern.format(prob))
        if os.path.exists(filename):
            df = pd.read_csv(filename)
            # Convert AckTime to numeric, skip NAs
            df['AckTime(ms)'] = pd.to_numeric(df['AckTime(ms)'], errors='coerce')
            avg_ack_time = df['AckTime(ms)'].mean()  # average time across frames
            results[scheme].append(avg_ack_time)
        else:
            print(f"Missing file: {filename}")
            results[scheme].append(np.nan)

# --- Prepare bar plot ---
x = np.arange(len(probs))
width = 0.25  # width of each bar

stop_vals = results['StopAndWait']
goback_vals = results['GoBackN']
selrep_vals = results['SelectiveRepeat']

plt.figure(figsize=(12, 6))

bars1 = plt.bar(x - width, stop_vals, width=width, label='Stop & Wait')
bars2 = plt.bar(x, goback_vals, width=width, label='Go-Back-N')
bars3 = plt.bar(x + width, selrep_vals, width=width, label='Selective Repeat')

# Add value labels on top of bars
for bars in [bars1, bars2, bars3]:
    for bar in bars:
        height = bar.get_height()
        if not np.isnan(height):
            plt.text(bar.get_x() + bar.get_width() / 2, height + 1,
                     f'{height:.1f}', ha='center', va='bottom', fontsize=8)

plt.xticks(x, probs)
plt.xlabel("Transmission Success Probability (%)")
plt.ylabel("Average Acknowledgement Time (ms)")
plt.title("Comparison of ARQ Schemes at Different Probabilities")
plt.legend()
plt.grid(axis='y', linestyle='--', alpha=0.7)

# --- Auto y-axis scaling ---
all_vals = [v for vals in results.values() for v in vals if not np.isnan(v)]
if len(all_vals) > 0:
    max_val = max(all_vals)
    if max_val <= 10:        # no timeouts
        plt.ylim(0, 15)
    elif max_val <= 5000:
        plt.ylim(0, 5500)
    else:
        plt.ylim(0, 10500)

plt.tight_layout()
out_img = os.path.join(base_path, "arq_comparison_histogram.png")
plt.savefig(out_img, dpi=300)
plt.show()

# --- Save performance metrics ---
metrics_df = pd.DataFrame({
    'Probability(%)': probs,
    'StopAndWait_avgAck(ms)': stop_vals,
    'GoBackN_avgAck(ms)': goback_vals,
    'SelectiveRepeat_avgAck(ms)': selrep_vals
})
out_csv = os.path.join(base_path, "performance_metrics.csv")
metrics_df.to_csv(out_csv, index=False)

print(f"Histogram saved as {out_img}")
print(f"Metrics saved as {out_csv}")
