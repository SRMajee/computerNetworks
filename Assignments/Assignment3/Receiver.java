package Assignments.Assignment3;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Receiver {
    private static int totalFrames = 0;
    private static final int MAX_RECEIVERS = 3;
    private static AtomicInteger frameCounter = new AtomicInteger(0);
    private static List<String> receivedFrames = Collections.synchronizedList(new ArrayList<>());

    // Statistics for performance measurement
    private static long simulationStartTime;
    private static long totalBitsReceived = 0;
    private static int successfulReceptions = 0;
    private static List<Long> receptionDelays = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Assignments.Assignment3.Receiver <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        Receiver receiver = new Receiver();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Receiver listening on port " + port);
            System.out.println("Waiting for CSMA-CD simulation requests...");

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    System.out.println("\nReceiver: Client connected: " + clientSocket.getInetAddress());

                    // Read protocol type and parameters
                    String protocol = in.readLine();
                    double persistenceProbability = Double.parseDouble(in.readLine());
                    totalFrames = Integer.parseInt(in.readLine());
                    long simulationDuration = Long.parseLong(in.readLine());

                    System.out.println("Protocol: " + protocol);
                    System.out.println("Persistence Probability: " + persistenceProbability);
                    System.out.println("Expected Frames: " + totalFrames);
                    System.out.println("Simulation Duration: " + simulationDuration + " ms");

                    if ("CSMA-CD".equals(protocol)) {
                        simulateCSMACD(in, out, persistenceProbability, simulationDuration);
                    } else {
                        System.err.println("Unknown protocol: " + protocol);
                    }

                } catch (IOException e) {
                    System.err.println("Error handling client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + port + ": " + e.getMessage());
        }
    }

    private static void simulateCSMACD(BufferedReader in, PrintWriter out, 
                                     double persistenceProbability, long simulationDuration) throws IOException {
        System.out.println("\n=== Starting p-persistent CSMA-CD Simulation ===");
        System.out.println("p = " + persistenceProbability);

        // Reset statistics
        frameCounter.set(0);
        receivedFrames.clear();
        totalBitsReceived = 0;
        successfulReceptions = 0;
        receptionDelays.clear();
        simulationStartTime = System.currentTimeMillis();

        // Create simulated channel with collision detection
        CSMAChannel channel = new CSMAChannel(persistenceProbability);

        // Process incoming frames
        List<String> incomingFrames = new ArrayList<>();
        String frame;
        while ((frame = in.readLine()) != null && incomingFrames.size() < totalFrames) {
            incomingFrames.add(frame);
            System.out.println("Received frame " + (incomingFrames.size()) + " from sender");
        }

        // Simulate CSMA-CD transmission for each frame
        for (int i = 0; i < incomingFrames.size(); i++) {
            String currentFrame = incomingFrames.get(i);
            boolean transmitted = channel.attemptTransmission(currentFrame, i + 1);

            if (transmitted) {
                successfulReceptions++;
                totalBitsReceived += currentFrame.length();
                receivedFrames.add(currentFrame);

                // Calculate simulated reception delay
                long delay = 50 + (long)(Math.random() * 100); // Simulated delay
                receptionDelays.add(delay);

                out.println("ACK_FRAME_" + (i + 1) + "_SUCCESS");
                System.out.println("Frame " + (i + 1) + " transmitted successfully");
            } else {
                out.println("ACK_FRAME_" + (i + 1) + "_COLLISION");
                System.out.println("Frame " + (i + 1) + " collision detected");
            }

            // Simulate transmission time
            try {
                Thread.sleep(20 + (long)(Math.random() * 30));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Generate statistics and CSV report
        generatePerformanceReport(persistenceProbability, simulationDuration);

        // Send final result
        double throughput = calculateThroughput(simulationDuration);
        double avgDelay = calculateAverageDelay();
        double efficiency = calculateEfficiency();

        String result = String.format("SIMULATION_COMPLETE:Throughput=%.2f,Delay=%.2f,Efficiency=%.2f", 
                                    throughput, avgDelay, efficiency * 100);
        out.println(result);

        System.out.println("\n=== Simulation Results ===");
        System.out.printf("Throughput: %.2f bits/second%n", throughput);
        System.out.printf("Average Delay: %.2f ms%n", avgDelay);
        System.out.printf("Efficiency: %.2f%%%n", efficiency * 100);
        System.out.printf("Successful Receptions: %d/%d%n", successfulReceptions, incomingFrames.size());
    }

    private static double calculateThroughput(long simulationDuration) {
        return (double) totalBitsReceived / (simulationDuration / 1000.0);
    }

    private static double calculateAverageDelay() {
        if (receptionDelays.isEmpty()) return 0.0;
        return receptionDelays.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private static double calculateEfficiency() {
        if (totalFrames == 0) return 0.0;
        return (double) successfulReceptions / totalFrames;
    }

    private static void generatePerformanceReport(double p, long simulationDuration) {
        try {
            String filename = "Assignments/Assignment3/performance_metrics_p" + 
                            String.format("%.1f", p).replace(".", "") + ".csv";

            FileWriter writer = new FileWriter(filename);
            writer.write("Metric,Value\n");
            writer.write("Persistence_Probability," + p + "\n");
            writer.write("Throughput," + calculateThroughput(simulationDuration) + "\n");
            writer.write("Average_Forwarding_Delay," + calculateAverageDelay() + "\n");
            writer.write("Efficiency," + calculateEfficiency() + "\n");
            writer.write("Total_Frames_Received," + successfulReceptions + "\n");
            writer.write("Total_Frames_Expected," + totalFrames + "\n");
            writer.write("Simulation_Duration_Ms," + simulationDuration + "\n");
            writer.close();

            System.out.println("Performance report saved: " + filename);

        } catch (IOException e) {
            System.err.println("Error writing performance report: " + e.getMessage());
        }
    }

    /**
     * Simulated CSMA Channel with collision detection
     */
    static class CSMAChannel {
        private double persistenceProbability;
        private Random random = new Random();
        private int totalTransmissions = 0;
        private int collisions = 0;

        public CSMAChannel(double p) {
            this.persistenceProbability = p;
        }

        public boolean attemptTransmission(String frame, int frameId) {
            totalTransmissions++;

            // Simulate p-persistent CSMA behavior
            if (random.nextDouble() > persistenceProbability) {
                // Defer transmission
                return false;
            }

            // Simulate collision probability (increases with higher p values)
            double collisionProbability = persistenceProbability * 0.3; // Scaled collision rate

            if (random.nextDouble() < collisionProbability) {
                collisions++;
                return false; // Collision occurred
            }

            return true; // Successful transmission
        }

        public double getCollisionRate() {
            return totalTransmissions == 0 ? 0.0 : (double) collisions / totalTransmissions;
        }
    }
}
