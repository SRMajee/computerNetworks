#!/usr/bin/env python3
"""
Analytics script for p-persistent CSMA-CD simulation
Generates performance comparison graphs
"""

import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import os

def load_simulation_data():
    """Load the combined simulation results"""
    csv_file = 'Assignments/Assignment3/combined_results.csv'

    if not os.path.exists(csv_file):
        print(f"Error: {csv_file} not found")
        return None

    try:
        df = pd.read_csv(csv_file)
        print(f"Loaded data for {len(df)} p values")
        return df
    except Exception as e:
        print(f"Error loading data: {e}")
        return None

def create_performance_comparison(df):
    """Create comprehensive performance comparison graphs"""

    # Create figure with subplots
    fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 10))
    fig.suptitle('p-persistent CSMA-CD Performance Analysis\n(Assignment 3 - Computer Networks Lab)', 
                 fontsize=16, fontweight='bold')

    # Plot 1: Throughput vs p
    ax1.plot(df['p'], df['Throughput'], 'b-o', linewidth=2, markersize=8, label='Throughput')
    ax1.set_xlabel('Persistence Probability (p)', fontsize=12)
    ax1.set_ylabel('Throughput (bits/sec)', fontsize=12)
    ax1.set_title('Throughput vs Persistence Probability', fontsize=14, fontweight='bold')
    ax1.grid(True, alpha=0.3)
    ax1.set_xlim(0, 1)

    # Plot 2: Average Delay vs p  
    ax2.plot(df['p'], df['Average_Delay'], 'r-s', linewidth=2, markersize=8, label='Avg Delay')
    ax2.set_xlabel('Persistence Probability (p)', fontsize=12)
    ax2.set_ylabel('Average Forwarding Delay (ms)', fontsize=12)
    ax2.set_title('Average Forwarding Delay vs Persistence Probability', fontsize=14, fontweight='bold')
    ax2.grid(True, alpha=0.3)
    ax2.set_xlim(0, 1)

    # Plot 3: Efficiency vs p
    efficiency_percent = df['Efficiency'] * 100
    ax3.plot(df['p'], efficiency_percent, 'g-^', linewidth=2, markersize=8, label='Efficiency')
    ax3.set_xlabel('Persistence Probability (p)', fontsize=12)
    ax3.set_ylabel('Channel Efficiency (%)', fontsize=12)
    ax3.set_title('Channel Efficiency vs Persistence Probability', fontsize=14, fontweight='bold')
    ax3.grid(True, alpha=0.3)
    ax3.set_xlim(0, 1)
    ax3.set_ylim(0, 100)

    # Plot 4: Collision Rate vs p
    collision_percent = df['Collision_Rate'] * 100
    ax4.plot(df['p'], collision_percent, 'm-d', linewidth=2, markersize=8, label='Collision Rate')
    ax4.set_xlabel('Persistence Probability (p)', fontsize=12)
    ax4.set_ylabel('Collision Rate (%)', fontsize=12)
    ax4.set_title('Collision Rate vs Persistence Probability', fontsize=14, fontweight='bold')
    ax4.grid(True, alpha=0.3)
    ax4.set_xlim(0, 1)
    ax4.set_ylim(0, max(100, collision_percent.max() * 1.1))

    plt.tight_layout()

    # Save the plot
    output_file = 'Assignments/Assignment3/performance_comparison.png'
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"Performance comparison graph saved: {output_file}")

    plt.show()

def create_throughput_delay_analysis(df):
    """Create throughput vs delay analysis"""

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
    fig.suptitle('Throughput and Delay Analysis\n(p-persistent CSMA-CD)', 
                 fontsize=16, fontweight='bold')

    # Throughput and Delay on same plot (different y-axes)
    ax1_twin = ax1.twinx()

    line1 = ax1.plot(df['p'], df['Throughput'], 'b-o', linewidth=2, markersize=8, label='Throughput')
    line2 = ax1_twin.plot(df['p'], df['Average_Delay'], 'r-s', linewidth=2, markersize=8, label='Delay')

    ax1.set_xlabel('Persistence Probability (p)', fontsize=12)
    ax1.set_ylabel('Throughput (bits/sec)', color='b', fontsize=12)
    ax1_twin.set_ylabel('Average Delay (ms)', color='r', fontsize=12)
    ax1.set_title('Throughput vs Delay Trade-off', fontsize=14, fontweight='bold')
    ax1.grid(True, alpha=0.3)

    # Combine legends
    lines = line1 + line2
    labels = [l.get_label() for l in lines]
    ax1.legend(lines, labels, loc='center right')

    # Scatter plot: Throughput vs Delay
    scatter = ax2.scatter(df['Average_Delay'], df['Throughput'], 
                         c=df['p'], s=100, cmap='viridis', alpha=0.7)

    # Add p value labels to points
    for i, p in enumerate(df['p']):
        ax2.annotate(f'p={p}', 
                    (df['Average_Delay'].iloc[i], df['Throughput'].iloc[i]),
                    xytext=(5, 5), textcoords='offset points', fontsize=10)

    ax2.set_xlabel('Average Forwarding Delay (ms)', fontsize=12)
    ax2.set_ylabel('Throughput (bits/sec)', fontsize=12)
    ax2.set_title('Throughput vs Delay Correlation', fontsize=14, fontweight='bold')
    ax2.grid(True, alpha=0.3)

    # Add colorbar
    cbar = plt.colorbar(scatter, ax=ax2)
    cbar.set_label('Persistence Probability (p)', fontsize=10)

    plt.tight_layout()

    # Save the plot
    output_file = 'Assignments/Assignment3/throughput_delay_analysis.png'
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"Throughput-Delay analysis graph saved: {output_file}")

    plt.show()

def print_performance_analysis(df):
    """Print detailed performance analysis"""

    print("\n" + "="*60)
    print("DETAILED PERFORMANCE ANALYSIS")
    print("="*60)

    # Find optimal values
    max_throughput_idx = df['Throughput'].idxmax()
    min_delay_idx = df['Average_Delay'].idxmin()
    max_efficiency_idx = df['Efficiency'].idxmax()
    min_collision_idx = df['Collision_Rate'].idxmin()

    print(f"\nOptimal p values:")
    print(f"  Maximum Throughput: p = {df.iloc[max_throughput_idx]['p']:.1f} ({df.iloc[max_throughput_idx]['Throughput']:.2f} bits/sec)")
    print(f"  Minimum Delay: p = {df.iloc[min_delay_idx]['p']:.1f} ({df.iloc[min_delay_idx]['Average_Delay']:.2f} ms)")
    print(f"  Maximum Efficiency: p = {df.iloc[max_efficiency_idx]['p']:.1f} ({df.iloc[max_efficiency_idx]['Efficiency']*100:.2f}%)")
    print(f"  Minimum Collisions: p = {df.iloc[min_collision_idx]['p']:.1f} ({df.iloc[min_collision_idx]['Collision_Rate']*100:.2f}%)")

    print(f"\nPerformance Summary:")
    print(f"{'p':<5} {'Throughput':<12} {'Delay(ms)':<10} {'Efficiency(%)':<12} {'Collisions(%)':<12}")
    print("-" * 55)

    for _, row in df.iterrows():
        print(f"{row['p']:<5.1f} {row['Throughput']:<12.2f} {row['Average_Delay']:<10.2f} "
              f"{row['Efficiency']*100:<12.2f} {row['Collision_Rate']*100:<12.2f}")

    print(f"\nKey Observations:")
    print(f"1. Low p values (≤0.3): Conservative approach with lower collisions but higher delays")
    print(f"2. Medium p values (0.4-0.6): Balanced performance across metrics")
    print(f"3. High p values (≥0.7): Aggressive approach with higher throughput but more collisions")
    print(f"\nRecommendation: Consider p = {df.iloc[max_efficiency_idx]['p']:.1f} for optimal efficiency")

def main():
    """Main analytics function"""

    print("p-persistent CSMA-CD Performance Analytics")
    print("=" * 45)

    # Load simulation data
    df = load_simulation_data()
    if df is None:
        return

    # Create visualizations
    create_performance_comparison(df)
    create_throughput_delay_analysis(df)

    # Print analysis
    print_performance_analysis(df)

    print(f"\nAnalysis complete!")
    print(f"Graph files saved in Assignments/Assignment3/")

if __name__ == "__main__":
    main()
