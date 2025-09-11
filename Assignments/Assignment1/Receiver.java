package Assignments.Assignment1;//package Assignments.Assignment1;

import static Assignments.Assignment1.Utils.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Receiver {
    private static final String[] SCHEMES = {"checksum", "crc8", "crc10", "crc16", "crc32"};
    private static final int[] CRC_WIDTHS = {8, 10, 16, 32}; // corresponding to crc8, crc10, crc16, crc32
    protected static List<List<Integer>> detectedFrames; // error_type is_detected for all types

    public Receiver() {
        detectedFrames = new ArrayList<>();
    }

    private static void checkFrame(ArrayList<String> currentFrame) {
        int frameNo = Integer.parseInt(currentFrame.get(0));
        int errorType = Integer.parseInt(currentFrame.get(1));

        // Ensure we have enough space in detectedFrames
        while (detectedFrames.size() <= frameNo) {
            detectedFrames.add(new ArrayList<>());
        }

        // Clear and initialize the frame entry: [errorType, checksum_result, crc8_result, crc10_result, crc16_result, crc32_result]
        detectedFrames.get(frameNo).clear();
        detectedFrames.get(frameNo).add(errorType);

        for (int i = 2; i <= 6; i++) {
            String frame = currentFrame.get(i);
            if (i == 2) {
                // Checksum validation
                detectedFrames.get(frameNo).add(validateCheckSum(frame));
            } else {
                // CRC validation (i-3 gives us index 0,1,2,3 for crc8,crc10,crc16,crc32)
                detectedFrames.get(frameNo).add(validateCrc(frame, CRC_WIDTHS[i - 3]));
            }
        }
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

            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Read total number of frames
                String totalFramesStr = in.readLine();
                int totalFrames = Integer.parseInt(totalFramesStr);
                System.out.println("Expecting " + totalFrames + " frames");

                // Process each frame one by one
                for (int expectedFrame = 0; expectedFrame < totalFrames; expectedFrame++) {
                    // ArrayList to store current frame data: [frameNumber, errorType, checksum, crc8, crc10, crc16, crc32]
                    ArrayList<String> currentFrame = new ArrayList<>();

                    // Read frame header
                    String frameLine = in.readLine();
                    String frameNumber = frameLine.split(":")[1];
                    currentFrame.add(frameNumber);

                    // Read error type
                    String errorLine = in.readLine();
                    String errorType = errorLine.split(":")[1];
                    currentFrame.add(errorType);

                    // Read all 5 schemes
                    for (int i = 0; i < 5; i++) {
                        String schemeLine = in.readLine();
                        String schemeData = schemeLine.split(":", 2)[1]; // Get data part after first ":"
                        currentFrame.add(schemeData);
                    }

                    // Read end frame marker
                    String endFrame = in.readLine();

                    // Now currentFrame contains: [frameNumber, errorType, checksum_data, crc8_data, crc10_data, crc16_data, crc32_data]
                    System.out.println("Received Frame " + frameNumber + " with error type " + errorType);

                    // Pass to your checking function here
                    checkFrame(currentFrame); // You can call your function here

                    // Send acknowledgment after processing this frame
                    out.println("ACK");
                }

                // Read end transmission
                String endTransmission = in.readLine();
                System.out.println("All frames received successfully!");

            } catch (IOException e) {
                System.err.println("Error handling client connection: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + port + ": " + e.getMessage());
        }
        // In your main, after END_TRANSMISSION:
        try {
            String csvPath = "Assignments/Assignment1/detected_frames.csv";
            exportDetectedFramesCsv(csvPath);

            System.out.println("CSV exported and Python analysis complete.");
        } catch (Exception e) {
            System.err.println("Failed to run post-processing: " + e.getMessage());
        }

        prettyDisplay();
    }

}

/*
javac Assignments\Assignment1\*.java
javac Assignments\Assignment1\Sender.java Assignments\Assignment1\Receiver.java
java Assignments.Assignment1.Receiver 5000
 */