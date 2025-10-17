package Assignments.Assignment4;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class Sender {
    private static class Result {
        int id;
        String original;
        String decoded;
        long latency;
        double throughput;
        double correctness;
    }

    private static class StationTask implements Runnable {
        private final int id;
        private final String bitsStr;
        private final WalshCode walsh;
        private final String host;
        private final int port;
        private final int bits;
        private final List<Result> results;
        private final CountDownLatch latch;

        public StationTask(int id,
                           String bitsStr,
                           WalshCode walsh,
                           String host,
                           int port,
                           int bits,
                           List<Result> results,
                           CountDownLatch latch) {
            this.id = id;
            this.bitsStr = bitsStr;
            this.walsh = walsh;
            this.host = host;
            this.port = port;
            this.bits = bits;
            this.results = results;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                // Generate spread sequence
                int[] code = walsh.getCode(id - 1);
                int[] seq = new int[bits * code.length];
                for (int b = 0; b < bits; b++) {
                    int v = bitsStr.charAt(b) == '1' ? 1 : -1;
                    for (int j = 0; j < code.length; j++) {
                        seq[b * code.length + j] = v * code[j];
                    }
                }

                // Open data socket
                Socket sock = new Socket(host, port);
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                // Send station ID
                out.println(id);

                // Send encoded sequence
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < seq.length; i++) {
                    sb.append(seq[i]);
                    if (i < seq.length - 1) sb.append(' ');
                }
                out.println(sb);

                // Measure round-trip
                long t0 = System.currentTimeMillis();
                String combined = in.readLine();
                long t1 = System.currentTimeMillis();
                long elapsed = t1 - t0;

                // Parse combined sequence
                String[] parts = combined.trim().split("\\s+");
                int[] sum = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();

                // Decode using Walsh code
                StringBuilder dec = new StringBuilder();
                for (int b = 0; b < bits; b++) {
                    int dot = 0;
                    for (int k = 0; k < code.length; k++) {
                        dot += sum[b * code.length + k] * code[k];
                    }
                    dec.append(dot > 0 ? '1' : '0');
                }

                // Compute metrics
                int correct = 0;
                for (int i = 0; i < bits; i++) {
                    if (dec.charAt(i) == bitsStr.charAt(i)) correct++;
                }
                double correctness = (double) correct / bits;
                double throughput = bits / (elapsed / 1000.0 + 1e-9);

                // Store result
                Result r = new Result();
                r.id = id;
                r.original = bitsStr;
                r.decoded = dec.toString();
                r.latency = elapsed;
                r.throughput = throughput;
                r.correctness = correctness;
                results.add(r);

                // Cleanup
                in.close();
                out.close();
                sock.close();
            } catch (Exception e) {
                System.err.println("Station " + id + ": " + e.getMessage());
            } finally {
                latch.countDown();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.err.println("Usage: java Sender <host> <port> <inputFile> <senderMAC> <receiverMAC> <numStations>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String inputFile = args[2];
        int n = Integer.parseInt(args[5]);

        if (n <= 0 || n > 100) {
            System.err.println("Invalid number of stations (must be between 1–100)");
            return;
        }

        // Read input lines
        List<String> data = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(Paths.get(inputFile))) {
                line = line.trim();
                if (!line.isEmpty()) data.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
            return;
        }

        int bits = data.get(0).length();
        System.out.println("Sender: using " + n + " stations with " + bits + " bits each");

        // Generate Walsh codes
        WalshCode walsh = new WalshCode(n);

        // Notify receiver of total stations
        try (Socket ctrl = new Socket(host, port);
             PrintWriter out = new PrintWriter(ctrl.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(ctrl.getInputStream()))) {
            out.println("TOTAL " + n);
            System.out.println("Sender control: " + in.readLine());
        }

        // Launch station threads
        CountDownLatch latch = new CountDownLatch(n);
        List<Result> results = Collections.synchronizedList(new ArrayList<>());

        for (int id = 1; id <= n; id++) {
            new Thread(new StationTask(id, data.get(id - 1), walsh, host, port, bits, results, latch)).start();
        }

        // Wait for all stations
        latch.await();

        // Write raw_results.csv
        try (PrintWriter pw = new PrintWriter("Assignments/Assignment4/raw_results.csv")) {
            pw.println("stationId,numBits,original,decoded,latency_ms,throughput_bps,correctness");
            for (Result r : results) {
                pw.printf("%d,%d,%s,%s,%d,%.2f,%.3f%n",
                        r.id, bits, r.original, r.decoded, r.latency, r.throughput, r.correctness);
            }
        }

        System.out.println("Sender done -> raw_results.csv");
    }
}
