#!/usr/bin/env python3
import pandas as pd, matplotlib.pyplot as plt, os, sys

INPUT = "Assignments/Assignment4/raw_results.csv"
OUT_CSV = "Assignments/Assignment4/performance_metrics.csv"
OUT_PNG = "Assignments/Assignment4/detection_histogram.png"

if not os.path.exists(INPUT):
    print("Missing raw_results.csv"); sys.exit(1)

df = pd.read_csv(INPUT)
avg_latency = df['latency_ms'].mean()
avg_throughput = df['throughput_bps'].mean()
avg_corr = df['correctness'].mean()
print(f"Stations: {len(df)}  Avg latency: {avg_latency:.2f} ms  "
      f"Throughput: {avg_throughput:.2f} bps  Correctness: {avg_corr:.3f}")

df[['stationId','latency_ms','throughput_bps','correctness']].to_csv(OUT_CSV, index=False)
plt.hist(df['correctness'], bins=10)
plt.xlabel('Detection correctness'); plt.ylabel('Stations')
plt.title('Per-station detection histogram')
plt.tight_layout(); plt.savefig(OUT_PNG)
print(f"Wrote {OUT_CSV}, {OUT_PNG}")
