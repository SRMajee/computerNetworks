package Assignments.Assignment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class Receiver {

    protected static int totalFrames;
    protected static List<List<Integer>> detectedFrames; // error_type is_detected for all types

    public Receiver() {
        detectedFrames = new ArrayList<>();
    }


    // Main
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Assignments.Assignment1.Receiver <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        Receiver receiver = new Receiver();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Receiver listening on port " + port);
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    System.out.println("Receiver : Client connected: " + clientSocket.getInetAddress());

                    // Read Flow Control Scheme
                    int ch = Integer.parseInt(in.readLine());
                    totalFrames = Integer.parseInt(in.readLine());
                    switch (ch) {
                        case 1:
                            stop_and_wait(in, out);
                            break;
                        case 2:
                            go_back_arq(in, out);
                            break;
                        case 3:
                            selective_repeat_arq(in, out);
                            break;
                        default:
                            break;
                    }
                } catch (IOException e) {
                    System.err.println("Error handling client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + port + ": " + e.getMessage());
        }
    }

    private static void stop_and_wait(BufferedReader in, PrintWriter out) throws IOException {
        List<String> frames = new ArrayList<>();
        Random random = new Random();
        int i = 0;

        while (true) {
            String frame = in.readLine();
            if (frame == null) break; // safety if sender closes connection
            frames.add(frame);

            System.out.println("Receiver : Received frame " + i);

            // Simulate ACK loss
            int rand = random.nextInt(100);
            if (rand < 95) { // 95% chance of sending ACK
                out.println("ACK");
                System.out.println("Receiver : ACK sent for frame " + i);
                i++;
            } else {
                // no ACK sent
                System.out.println("Receiver : Dropped ACK for frame " + i);
            }
        }
    }


    private static void go_back_arq(BufferedReader in, PrintWriter out) throws IOException {
        Random random = new Random();
        int expectedFrameNo = 0;

        while (true) {
            String frame = in.readLine();

            if (frame == null) break; // safety if sender closes connection

            int recievedFrameNo = Integer.parseInt(frame.split(":")[0]);

            frame = frame.split(":")[1];

            if (frame == null) break;

            if (recievedFrameNo == expectedFrameNo) {
                System.out.println("Receiver : Frame " + expectedFrameNo + " received in order");

                // Simulate ACK loss
                if (random.nextInt(100) < 95) { // 95% chance send ACK
                    sendACK(out, random, expectedFrameNo);  // cumulative ACK
                    System.out.println("Receiver : Sent ACK " + expectedFrameNo);
                } else {
                    System.out.println("Receiver : Dropped ACK " + expectedFrameNo);
                }
                expectedFrameNo++;
            } else {
                // wrong frame → do not do anything
                System.out.println("Receiver : Out of order frame detected... expected frame " + expectedFrameNo + " receiver frame " + recievedFrameNo);
                sendACK(out, random, expectedFrameNo - 1); // duplicate ACK
                System.out.println("Receiver : Resent ACK for frame " + (expectedFrameNo - 1));
            }
        }
    }


    private static void selective_repeat_arq(BufferedReader in, PrintWriter out) throws IOException {
        Random random = new Random();
        boolean[] received = new boolean[totalFrames];
        String[] buffer = new String[totalFrames];

        int expectedFrame = 0;
        Integer pendingNak = null;

        while (true) {
            String line = in.readLine();
            if (line == null) break;

            int frameNo = Integer.parseInt(line.split(":")[0]);
            String data = line.split(":")[1];

            // Duplicate frame handler
            if (received[frameNo]) {
                // Already received → re-send last ACK
                int ackToSend = expectedFrame > 0 ? expectedFrame - 1 : 0;
                System.out.println("Receiver : Duplicate frame " + frameNo + " detected, re-sending ACK " + ackToSend);
                sendACK(out, random, ackToSend);  // duplicate ACK here
                continue;  // Skip normal processing
            }

            // New Frame Handler
            received[frameNo] = true;
            buffer[frameNo] = data;
            System.out.println("Receiver : Frame " + frameNo + " received and buffered");

            if (frameNo == expectedFrame) {
                // Got the expected frame
                expectedFrame++;
                // Move forward through any already-buffered frames
                while (expectedFrame < totalFrames && received[expectedFrame]) {
                    expectedFrame++;
                }

                // If previously there was a NAK, and it is now filled → send cumulative ACK
                if (pendingNak != null) {
                    pendingNak = null;
                    sendACK(out, random, expectedFrame - 1);
                    System.out.println("Receiver : Sent cumulative ACK " + (expectedFrame - 1));
                } else {
                    // Normal ACK if no NAK pending
                    sendACK(out, random, expectedFrame - 1);
                    System.out.println("Receiver : ACK sent for frame " + (expectedFrame - 1));
                }
            } else if (frameNo > expectedFrame) {
                // Out-of-order → issue NAK for expectedFrame
                if (pendingNak == null) {
                    pendingNak = expectedFrame;
                    System.out.println("Receiver : Out of order detected, sending NAK " + expectedFrame);
                    sendNAK(out, random, expectedFrame);
                }
                // Hold ACKs until missing frame arrives
            }
        }
    }


    private static void sendACK(PrintWriter out, Random random, int ackNum) {
        if (random.nextInt(100) < 95) {
            out.println("ACK:" + ackNum);
        } else {
            System.out.println("Receiver : Dropped ACK " + ackNum);
        }
    }


    private static void sendNAK(PrintWriter out, Random random, int nakNum) {
        if (random.nextInt(100) < 95) {
            out.println("NAK:" + nakNum);
        } else {
            System.out.println("Receiver : Dropped NAK " + nakNum);
        }
    }

}


/*
javac Assignments\Assignment2\Sender.java Assignments\Assignment2\Receiver.java
java Assignments.Assignment2.Receiver 5000
 */