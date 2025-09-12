package Assignments.Assignment2;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static Assignments.Assignment1.Utils.exportToCSV;

import static Assignments.Assignment2.Sender.host;
import static Assignments.Assignment2.Sender.port;
import static Assignments.Assignment2.Sender.frameList;
import static Assignments.Assignment2.Sender.TOTAL_FRAMES;
import static Assignments.Assignment2.Sender.TIMEOUT_MS;
import static Assignments.Assignment2.Sender.PROB;

public class StopAndWait {
//    final int TIMEOUT_MS = 5000; // timeout after 5000ms
//    final int TOTAL_FRAMES = 5;
//    final int PROB = 95;

    // store time taken for each frame
    private final Map<Integer, Long> frameTimes = new LinkedHashMap<>();
    // store the first send time for each frame index
    private final Map<Integer, Long> sendTimeMap = new HashMap<>();

    public StopAndWait() throws IOException {
        run();
    }

    protected void run() throws IOException {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Sender : Connected to receiver at " + host + ":" + port);

            // tell receiver : Stop And Wait
            out.println("1");
            out.println(TOTAL_FRAMES);

            long totalTime = 0;
            socket.setSoTimeout(TIMEOUT_MS);
            Random random = new Random();

            for (int i = 0; i < frameList.size() && i < TOTAL_FRAMES; i++) {
                boolean ackReceived = false;

                while (!ackReceived) {
                    sendFrame(i, random, out);
                    startTimer(socket);

                    try {
                        ackReceived = recvAck(i, in, socket);
                    } catch (SocketTimeoutException e) {
                        handleTimeout(i);
                    }
                }

                totalTime += frameTimes.get(i);
            }

            System.out.println("==================================================");
            System.out.println("Sender : Total transmission time for all frames: " + totalTime + " ms");
            String fileName = String.format("Assignments/Assignment2/csvframe_times_stop_and_wait%d.csv", PROB);
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
        if (!sendTimeMap.containsKey(frameNumber)) sendTimeMap.putIfAbsent(frameNumber, System.currentTimeMillis());
    }


    private void startTimer(Socket socket) throws IOException {
        socket.setSoTimeout(TIMEOUT_MS);
        System.out.println("\nSender : Timer Started ...\n");
    }

    // Handles retransmission
    private void handleTimeout(int i) {
        System.out.println("Sender : Timeout: No ACK for frame " + i + " within "
                + TIMEOUT_MS / 1000 + "s");
        System.out.println("Sender : Retransmitting frame " + i + "...");
    }

    // Receives an ACK and computes the RTT/time taken for the frame.
    private boolean recvAck(int i, BufferedReader in, Socket socket) throws IOException {
        String ack = in.readLine();
        long endTime = System.currentTimeMillis();

        if ("ACK".equals(ack)) {
            long sentAt = sendTimeMap.get(i);
            long timeTaken = endTime - sentAt;
            frameTimes.put(i, timeTaken);

            System.out.println("Sender : Frame " + i + " acknowledged. Time taken: "
                    + frameTimes.get(i) + " ms");

            socket.setSoTimeout(TIMEOUT_MS);
            System.out.println("\nSender : Timer Restarted ...\n");

            return true;
        }
        return false;
    }
}
