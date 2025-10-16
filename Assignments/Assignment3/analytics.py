import pandas as pd
import matplotlib.pyplot as plt
import glob
import os

base_path = "Assignments/Assignment3"
# Path where CSVs are stored
csv_folder = "./"  # same folder as the script, adjust if needed
csv_files = glob.glob(
    os.path.join(csv_folder, "csma_cd_results_*.csv")
)  # e.g., csma_cd_results_0.5.csv

ps = []
throughputs = []
delays = []
effs = []
for file in csv_files:
    # Extract p from filename, e.g., csma_cd_results_0.5.csv
    basename = os.path.basename(file)
    p_str = basename.split("_")[-1].replace(".csv", "")
    p_val = float(p_str)

    df = pd.read_csv(file)
    ps.append(p_val)
    bits_succ = df["bitsSuccessful"].iloc[0]
    bits_att = df["bitsAttempted"].iloc[0]
    efficiency = bits_succ / bits_att if bits_att > 0 else 0
    effs.append(efficiency)
    throughputs.append(df["throughputBps"].iloc[0])
    delays.append(df["avgDelayMs"].iloc[0])

# Sort by p
sorted_indices = sorted(range(len(ps)), key=lambda k: ps[k])
ps = [ps[i] for i in sorted_indices]
throughputs = [throughputs[i] for i in sorted_indices]
delays = [delays[i] for i in sorted_indices]
effs = [effs[i] for i in sorted_indices]

# Plot Throughput vs p
plt.figure(figsize=(8, 5))
plt.plot(ps, throughputs, marker="o", linestyle="-", color="b")
plt.xlabel("Persistence Probability p")
plt.ylabel("Throughput (bps)")
plt.title("Throughput vs Persistence Probability p")
plt.grid(True)
plt.savefig("throughput_vs_p.png")
plt.show()

# Plot Forwarding Delay vs p
plt.figure(figsize=(8, 5))
plt.plot(ps, delays, marker="s", linestyle="-", color="r")
plt.xlabel("Persistence Probability p")
plt.ylabel("Average Forwarding Delay (ms)")
plt.title("Forwarding Delay vs Persistence Probability p")
plt.grid(True)
plt.savefig("forwarding_delay_vs_p.png")
plt.show()

# Create efficiency plot
plt.figure(figsize=(8, 5))
plt.plot(ps, effs, marker="o", linestyle="-", color="g")
plt.xlabel("Persistence Probability p")
plt.ylabel("Efficiency (successful/attempted bits)")
plt.title("Protocol Efficiency vs p")
plt.grid(True)
plt.tight_layout()
plt.savefig("efficiency_vs_p.png")
plt.show()
print("Saved efficiency_vs_p.png")
