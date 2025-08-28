import sys
import pandas as pd
import matplotlib
matplotlib.use('Agg')  # Use non-GUI backend
import matplotlib.pyplot as plt
import numpy as np

def main(csv_path):
    df = pd.read_csv(csv_path)
    schemes = ['checksum','crc8','crc10','crc16','crc32']
    error_types = df['errorType'].unique()
    error_type_names = {0: 'None', 1: 'Single', 2: 'Two', 3: 'Odd', 4: 'Burst'}

    # Calculate detection rates per error type and scheme (percentage)
    grouped = df.groupby('errorType')[schemes].mean() * 100
    grouped = grouped.loc[sorted(grouped.index)]  # sort by error type for consistent plotting

    # Plot grouped bar chart
    labels = [error_type_names.get(e, str(e)) for e in grouped.index]
    x = np.arange(len(labels))  # label locations
    width = 0.15  # width of the bars

    fig, ax = plt.subplots(figsize=(10,6))
    for i, scheme in enumerate(schemes):
        ax.bar(x + i*width, grouped[scheme], width, label=scheme.capitalize())

    ax.set_ylabel('Detection Rate (%)')
    ax.set_title('Detection Rate per Error Type and Scheme')
    ax.set_xticks(x + width * (len(schemes)-1) / 2)
    ax.set_xticklabels(labels)
    ax.set_ylim(0, 110)
    ax.legend()
    ax.grid(axis='y', linestyle='--', alpha=0.7)

    plt.tight_layout()
    plt.savefig('detection_histogram.png')
    plt.close()

    # Save performance metrics CSV as before
    metrics = []
    for etype, group in df.groupby('errorType'):
        rates = group[schemes].mean() * 100
        row = {'errorType': error_type_names.get(etype, etype)}
        row.update(rates.to_dict())
        metrics.append(row)
    pd.DataFrame(metrics).sort_values('errorType') \
      .to_csv('performance_metrics.csv', index=False)

    print("Generated detection_histogram.png and performance_metrics.csv")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 analytics.py detected_frames.csv")
        sys.exit(1)
    main(sys.argv[1])
