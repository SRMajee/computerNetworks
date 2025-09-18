package Assignments.Assignment3;

import java.io.*;
import java.net.*;
import java.util.*;

import static Assignments.Assignment2.FrameBuilder.createFrames;

public class Sender {
    private String inputFilePath, senderMacAddress, receiverMacAddress;
    private static final String LEN_BIN = "00101110" + "00101110"; // 46 decimal
    protected static List<String> frameList; // frames for CSMA-CD transmission
    protected static int port;
    protected static String host;
    protected static final int N = 3;

    // CSMA-CD specific parameters
    private static final double[] P_VALUES = {0.1, 0.3, 0.5, 0.7, 0.9};
    private static final long SIMULATION_DURATION = 15000; // 15 seconds

    public Sender(String inputFilePath, String senderMAC, String receiverMAC) throws IOException {
        this.inputFilePath = inputFilePath;
        this.senderMacAddress = macToBinary(senderMAC);
        this.receiverMacAddress = macToBinary(receiverMAC);
        frameList = createFrames(inputFilePath, senderMacAddress, receiverMacAddress, LEN_BIN);

        System.out.println("Sender initialized with " + frameList.size() + " frames");
    }

    private static String macToBinary(String mac) {
        return Arrays.stream(mac.split("-"))
                .map(h -> String.format("%8s", Integer.toBinaryString(Integer.parseInt(h, 16)))
                        .replace(' ', '0'))
                .reduce("", String::concat);
    }
    public static void main(String[] args) throws IOException {
        if (args.length < 5) {
            System.err.println("Usage: java Assignments.Assignment3.Sender <host> <port> <inputfile> <senderMac> <receiverMac>");
            return;
        }

        host = args[0];
        port = Integer.parseInt(args[1]);
        String inputFilePath = args[2];
        String senderMAC = args[3];
        String receiverMAC = args[4];

        Sender sender = new Sender(inputFilePath, senderMAC, receiverMAC);

        outer:
        while (true) {
            System.out.print("Enter \n1. p-persistent CSMA-CD (p=0.1)  \n2. p-persistent CSMA-CD (p=0.3)  \n3. p-persistent CSMA-CD (p=0.5)  \n4. p-persistent CSMA-CD (p=0.7)  \n5. p-persistent CSMA-CD (p=0.9)  \n6. Run All p values  \n0. To Exit\nEnter choice : ");
            Scanner sc = new Scanner(System.in);
            int ch = sc.nextInt();

            switch (ch) {
                case 1:
                    runCSMACD(0.1);
                    break;
                case 2:
                    runCSMACD(0.3);
                    break;
                case 3:
                    runCSMACD(0.5);
                    break;
                case 4:
                    runCSMACD(0.7);
                    break;
                case 5:
                    runCSMACD(0.9);
                    break;
                case 6:
                    runAllPValues();
                    break;
                default:
                    break outer;
            }
        }
    }

    private static void runCSMACD(double p) {
        System.out.println("\n=== Running p-persistent CSMA-CD with p = " + p + " ===");

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to Receiver at " + host + ":" + port);

            // Send protocol choice and parameters
            out.println("CSMA-CD"); // Protocol type
            out.println(p); // Persistence probability
            out.println(frameList.size()); // Number of frames
            out.println(SIMULATION_DURATION); // Simulation duration

            // Send all frames
            for (int i = 0; i < frameList.size(); i++) {
                String frame = frameList.get(i);
                out.println(frame);
                System.out.println("Sent frame " + (i + 1) + " (length: " + frame.length() + " bits)");

                // Wait for acknowledgment
                String ack = in.readLine();
                if (ack != null && ack.startsWith("ACK")) {
                    System.out.println("Received: " + ack);
                } else {
                    System.out.println("No acknowledgment received for frame " + (i + 1));
                }

                // Small delay between frames
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Wait for simulation completion
            String result = in.readLine();
            if (result != null) {
                System.out.println("Simulation result: " + result);
            }

        } catch (IOException e) {
            System.err.println("Error communicating with receiver: " + e.getMessage());
        }
    }

    private static void runAllPValues() {
        System.out.println("\n=== Running p-persistent CSMA-CD for all p values ===");

        for (double p : P_VALUES) {
            runCSMACD(p);

            // Delay between different p value runs
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n=== All simulations completed ===");
        System.out.println("Check Assignments/Assignment3/ for generated CSV files and graphs");
    }
}
