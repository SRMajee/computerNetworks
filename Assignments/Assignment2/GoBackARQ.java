package Assignments.Assignment2;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

import static Assignments.Assignment1.Utils.exportToCSV;
import static Assignments.Assignment2.Sender.*;

public class GoBackARQ {
//    final int TIMEOUT_MS = 5000; // timeout after 5000ms
//    final int TOTAL_FRAMES = 5;

    // store time taken for each frame
    private final Map<Integer, Long> frameTimes = new LinkedHashMap<>();
    // store the first send time for each frame index
    private final Map<Integer, Long> sendTimeMap = new HashMap<>();

    public GoBackARQ() throws IOException {
        run();
    }

    protected void run() throws IOException {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Sender : Connected to receiver at " + host + ":" + port);

            // tell receiver : GoBackN
            out.println("2");

            int start = 0;     // first unacknowledged frame
            int i = 0;         // next frame to send
            long totalTime = 0; // total transfer time across all frames
            Random random = new Random();

            startTimer(socket);

            while (start < TOTAL_FRAMES) {
                // send frames within window
                while (i < start + N && i < TOTAL_FRAMES) {
                    sendFrame(i, random, out);
                    i++;
                }

                try {
                    int ackNum = recvAck(in, start, totalTime); // returns cumulative ACK number
                    if (ackNum >= 0) {
                        // record the time taken for all frames up to ackNum that are not yet recorded
                        for (int f = start; f <= ackNum && f < TOTAL_FRAMES; f++) {
                            if (!frameTimes.containsKey(f)) {
                                Long sentAt = sendTimeMap.get(f);
                                long timeTaken = System.currentTimeMillis() - sentAt;
                                frameTimes.put(f, timeTaken);
                                totalTime += timeTaken;
                                System.out.println("Sender : Time taken for frame " + f + ": " + timeTaken + " ms");
                            }
                        }

                        // slide window
                        start = ackNum + 1;
                        System.out.println("Sender : Increasing window start to " + start);
                        startTimer(socket);
                    }
                } catch (SocketTimeoutException e) {
                    i = handleTimeout(start);
                }
            }

            System.out.println("==================================================");
            System.out.println("Sender : Total transmission time for all frames: " + totalTime + " ms");
            String fileName = String.format("Assignments/Assignment2/csvframe_times_go_back_n%d.csv", PROB);
//            exportToCSV(sendTimeMap,frameTimes,fileName);
        }
    }

    private void sendFrame(int frameNumber, Random random, PrintWriter out) {
        String frame = frameList.get(frameNumber);
        if (random.nextInt(100) < PROB) {
            System.out.println("Sender : Successfully Sent frame " + frameNumber);
            out.println(frameNumber + ":" + frame);
        } else {
            System.out.println("Sender : Frame " + frameNumber + " lost in transmission... (simulating loss)");
        }
        // record first send time only
        sendTimeMap.putIfAbsent(frameNumber, System.currentTimeMillis());
    }

    private void startTimer(Socket socket) throws IOException {
        socket.setSoTimeout(TIMEOUT_MS);
        System.out.println("\nSender : Timer Started/Restarted ...\n");
    }

    // Handles retransmission
    private int handleTimeout(int start) {
        System.out.println("Sender : Timeout retransmitting from frame " + start);
        return start; // Reset index to first unacknowledged frame
    }

    // Receives an ACK and computes the RTT/time taken for the frame.
    private int recvAck(BufferedReader in, int start, long totalTime) throws IOException {
        String ack = in.readLine();
        if (ack == null) return -1;

        if (ack.startsWith("ACK")) {
            int ackNum = Integer.parseInt(ack.split(":")[1]); // e.g., "ACK:3"
            System.out.println("Sender : Received cumulative ACK for frame " + ackNum);
            return ackNum;
        }
        return -1;
    }

    public Map<Integer, Long> getFrameTimes() {
        return frameTimes;
    }
}
