package Assignments.Assignment2;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

import static Assignments.Assignment1.Utils.exportToCSV;
import static Assignments.Assignment2.Sender.*;

public class SelectiveRepeatARQ {
//    final int TIMEOUT_MS = 5000;
//    final int TOTAL_FRAMES = 7;

    // store time taken for each frame
    private final Map<Integer, Long> frameTimes = new LinkedHashMap<>();
    // store the first send time for each frame index
    private final Map<Integer, Long> sendTimeMap = new HashMap<>();
    long totalTime = 0;

    public SelectiveRepeatARQ() throws IOException {
        run();
    }

    protected void run() throws IOException {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Sender : Connected to receiver at " + host + ":" + port);

            // tell receiver : Selective Repeat
            out.println("3");
            out.println(TOTAL_FRAMES);

            Random random = new Random();
            LinkedHashSet<Integer> window = new LinkedHashSet<>();
            int nextFrame = 0;

            // fill initial window
            for (int j = 0; j < N && j < TOTAL_FRAMES; j++) {
                window.add(nextFrame++);
            }

            // send initial window
            for (int f : window) {
                sendFrame(f, random, out);
            }


            startTimer(socket);

            while (!window.isEmpty()) {
                try {
                    String res = in.readLine();
                    if (res == null) break;
                    recvResponse(res, window, out, random, socket); // handle ACK/NAK
                } catch (SocketTimeoutException e) {
                    handleTimeout(window, out, random, socket);
                }
            }

            System.out.println("==================================================");
            System.out.println("Sender : Total transmission time for all frames: " + totalTime + " ms");
            String fileName = String.format("Assignments/Assignment2/csvframe_times_selective_repeat%d.csv", PROB);
//            exportToCSV(sendTimeMap, frameTimes, fileName);
        }
    }

    private void sendFrame(int i, Random random, PrintWriter out) {
        String frame = frameList.get(i);
        if (random.nextInt(100) < PROB) {
            System.out.println("Sender : Successfully Sent frame " + i);
            out.println(i + ":" + frame);
        } else {
            System.out.println("Sender : Frame " + i + " lost in transmission... (simulating loss)");
        }
        if (!sendTimeMap.containsKey(i)) sendTimeMap.putIfAbsent(i, System.currentTimeMillis());
    }


    private void startTimer(Socket socket) throws IOException {
        socket.setSoTimeout(TIMEOUT_MS);
        System.out.println("\nSender : Timer Started/Restarted ...\n");
    }

    // Handles retransmission
    private void handleTimeout(LinkedHashSet<Integer> window, PrintWriter out, Random random, Socket socket) throws IOException {
        System.out.println("Sender : Timeout! Retransmitting all unacknowledged frames in window");
        for (int f : window) {
            sendFrame(f, random, out);
        }
        startTimer(socket);
    }

    // Receives an ACK and computes the RTT/time taken for the frame.
    private void recvResponse(String res, LinkedHashSet<Integer> window,
                              PrintWriter out, Random random, Socket socket) throws IOException {
        if (res.startsWith("ACK")) {
            int ackNum = Integer.parseInt(res.split(":")[1]);
            System.out.println("Sender : Received cumulative ACK for frame " + ackNum);

            // find which frames in window are <= ackNum
            List<Integer> ackedFrames = new ArrayList<>();
            for (int f : window) {
                if (f <= ackNum && !frameTimes.containsKey(f)) {
                    long timeTaken = System.currentTimeMillis() - sendTimeMap.get(f);
                    frameTimes.put(f, timeTaken);
                    totalTime += timeTaken;
                    System.out.println("Sender : Time taken for frame " + f + ": " + timeTaken + " ms");
                    ackedFrames.add(f);
                }
                if (f > ackNum) break;
            }

            // Slide window: remove acked frames and insert new frames
            int nextFrame = (window.isEmpty()) ? ackNum + 1 : Collections.max(window) + 1;
            for (int f : ackedFrames) {
                window.remove(f);
                if (nextFrame < TOTAL_FRAMES) {
                    window.add(nextFrame);
                    sendFrame(nextFrame, random, out);
                    nextFrame++;
                }
            }

            System.out.println("Sender : Current window: " + window);
            startTimer(socket);
        } else if (res.startsWith("NAK")) {
            int nakNum = Integer.parseInt(res.split(":")[1]);
            System.out.println("Sender : NAK received for frame " + nakNum);
            sendFrame(nakNum, random, out);
        }
    }

    public Map<Integer, Long> getFrameTimes() {
        return frameTimes;
    }
}
