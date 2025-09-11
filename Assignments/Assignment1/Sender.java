package Assignments.Assignment1;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

import static Assignments.Assignment1.FrameBuilder.createFrames;

public class Sender {
    private static final String[] ERROR_TYPES = {"none", "single", "two", "odd", "burst"};
    private static final String[] SCHEMES = { "checksum","crc8", "crc10", "crc16", "crc32"};
    private String inputFilePath, senderMaxAddress, recieveMacAddress;
    private static final String LEN_BIN = "00101110" + "00101110"; // 46 decimal
    protected List<List<String>> frameList; // frame 1 -> 5 frames for different schems with error injected in 4/5 out of them

    public Sender(String inputFilePath, String senderMAC, String recieverMAC) throws IOException {
        this.inputFilePath = inputFilePath;
        this.senderMaxAddress = macToBinary(senderMAC);
        this.recieveMacAddress = macToBinary(recieverMAC);
        frameList = createFrames(inputFilePath, senderMaxAddress, recieveMacAddress, LEN_BIN);
    }

    private static String macToBinary(String mac) {
        return Arrays.stream(mac.split("-"))
                .map(h -> String.format("%8s", Integer.toBinaryString(Integer.parseInt(h, 16)))
                        .replace(' ', '0'))
                .reduce("", String::concat);
    }


    public static void main(String[] args) throws IOException {
        if (args.length < 5) {
            System.err.println("Usage: java Assignments.Assignment1.Sender <host> <port> <inputfile> <senderMac> <recieverMac>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String inputFilePath = args[2];
        String senderMAC = args[3];
        String recieverMAC = args[4];
        Sender sender = new Sender(inputFilePath, senderMAC, recieverMAC);
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to receiver at " + host + ":" + port);

            // Send total number of frames first
            out.println(sender.frameList.size());

            // Send each frame with all 5 schemes and corresponding error injection
            for (int i = 0; i < sender.frameList.size(); i++) {
                List<String> currentFrameSchemes = sender.frameList.get(i);
                int errorNo = i%5;
                String errorType = ERROR_TYPES[errorNo]; // 0=none, 1=single, 2=two, 3=odd, 4=burst

                // Send frame number
                out.println("FRAME:" + i);

                // Send error type
                out.println("ERROR_TYPE:" + errorNo);

                // Send all 5 schemes for this frame
                out.println("CHECKSUM:" + currentFrameSchemes.get(0));
                out.println("CRC8:" + currentFrameSchemes.get(1));
                out.println("CRC10:" + currentFrameSchemes.get(2));
                out.println("CRC16:" + currentFrameSchemes.get(3));
                out.println("CRC32:" + currentFrameSchemes.get(4));

                // Send end of frame marker
                out.println("END_FRAME");

                // Wait for acknowledgment from receiver
                String ack = in.readLine();
                if ("ACK".equals(ack)) {
                    System.out.println("Frame " + i + " sent successfully with error type " + errorType);
                } else {
                    System.out.println("Error sending frame " + i + ": " + ack);
                }
            }
        }
    }
}

// java Assignments.Assignment1.Sender localhost 5000 Assignments/Assignment1/inputfile.txt 98-BA-5F-ED-66-B7 AA-BA-5F-ED-66-B7
