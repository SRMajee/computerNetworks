package Assignments.Assignment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static Assignments.Assignment2.FrameBuilder.createFrames;

public class Sender {
    private String inputFilePath, senderMacAddress, recieverMacAddress;
    private static final String LEN_BIN = "00101110" + "00101110"; // 46 decimal
    protected static List<String> frameList; // frame 1 -> 5 frames for different schems with error injected in 4/5 out of them
    protected static int  port;
    protected static String host;
    protected static final int TIMEOUT_MS = 5000; // timeout after 5000ms
    protected static final int TOTAL_FRAMES = 7;
    protected static final int PROB =95;
    protected static final int N = 3;


    public Sender(String inputFilePath, String senderMAC, String recieverMAC) throws IOException {
        this.inputFilePath = inputFilePath;
        this.senderMacAddress = macToBinary(senderMAC);
        this.recieverMacAddress = macToBinary(recieverMAC);
        frameList = createFrames(inputFilePath, senderMacAddress, recieverMacAddress, LEN_BIN);
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

        host = args[0];
        port = Integer.parseInt(args[1]);
        String inputFilePath = args[2];
        String senderMAC = args[3];
        String recieverMAC = args[4];
        Sender sender = new Sender(inputFilePath, senderMAC, recieverMAC);
        outer:
        while (true) {
            System.out.print("Enter \n1. Stop and Wait  \n2. Go-Back-N ARQ  \n3. Selective Repeat ARQ  \n0. To Exit\nEnter choice : ");
            Scanner sc = new Scanner(System.in);
            int ch = sc.nextInt();

            switch (ch) {
                case 1:
                    new StopAndWait();
                    break;
                case 2:
                    new GoBackARQ();
                    break;
                case 3:
                    new SelectiveRepeatARQ();
                    break;
                default:
                    break outer;
            }
        }
    }
}

// java Assignments.Assignment2.Sender localhost 5000 Assignments/Assignment2/inputfile.txt 98-BA-5F-ED-66-B7 AA-BA-5F-ED-66-B7
