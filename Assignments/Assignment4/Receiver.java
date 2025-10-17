package Assignments.Assignment4;

import java.io.*;
import java.net.*;
import java.util.*;

public class Receiver {
    private static volatile int expectedStations = -1;
    private static final Object lock = new Object();
    private static List<int[]> sequences = new ArrayList<>();
    private static List<Socket> clients = new ArrayList<>();
    private static int completedThreads=0;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Assignments.Assignment4.Receiver <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Receiver listening on port " + port);

        while (true) {
            Socket sock = serverSocket.accept();
            new Thread(() -> handle(sock)).start();
        }
    }

    private static void handle(Socket socket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line1 = in.readLine();
            if (line1 == null) return;

            if (line1.startsWith("TOTAL")) {
                expectedStations = Integer.parseInt(line1.split("\\s+")[1]);
                out.println("ACK TOTAL " + expectedStations);
                System.out.println("Receiver: expecting " + expectedStations + " stations");
                return;
            }

            int stationId = Integer.parseInt(line1.trim());
            String dataLine = in.readLine();
            String[] p = dataLine.trim().split("\\s+");
            int[] seq = new int[p.length];
            for (int i = 0; i < p.length; i++) seq[i] = Integer.parseInt(p[i]);

            int[] combined = null;
            synchronized (lock) {
                clients.add(socket);
                sequences.add(seq);
                if (sequences.size() == expectedStations) {
                    int len = seq.length;
                    combined = new int[len];
                    for (int[] s : sequences)
                        for (int i = 0; i < len; i++) combined[i] += s[i];
                    sequences.clear();
                    sequences.add(combined);
                    lock.notifyAll();
                } else {
                    lock.wait();
                }
            }

            int[] finalCombined;
            synchronized (lock) {
                finalCombined = sequences.get(0);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < finalCombined.length; i++) {
                sb.append(finalCombined[i]);
                if (i < finalCombined.length - 1) sb.append(' ');
            }
            out.println(sb.toString());
            socket.close();

            synchronized (lock) {
                completedThreads++;
                if (completedThreads == expectedStations) {
                    clients.clear();
                    sequences.clear();
                    expectedStations = -1;
                    completedThreads = 0;  // Reset for next round
                    System.out.println("Receiver: round complete.");
                }
            }

        } catch (Exception e) {
            System.err.println("Receiver error: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
