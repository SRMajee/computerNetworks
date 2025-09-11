package Assignments.Assignment2;


import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static Assignments.Assignment1.Utils.getChecksum;

public class FrameBuilder {


    public static class Frame {
        private final String bits;    // 480-bits string of '0'/'1'

        public Frame(String bits) {
            this.bits = bits;
        }

        public String getBits() {
            return bits;
        }

        @Override
        public String toString() {
            return bits;
        }
    }

    static List<Frame> frames;
    static List<String> correctedFrames;

    // create frames of 60 Bytes
    public static List<String> createFrames(
            String inputFilePath,
            String senderMacAddress,
            String receiveMacAddress,
            String LEN
    ) throws IOException {
        final int PAYLOAD_CHARS = 46;
        final int FRAME_BITS = 60 * 8;  // 480 bits
        frames = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFilePath))) {
            char[] buffer = new char[PAYLOAD_CHARS];
            int readCount;
            while ((readCount = reader.read(buffer)) != -1) {
                // Convert up to 46 chars to bits
                StringBuilder payloadBits = new StringBuilder(PAYLOAD_CHARS * 8);
                for (int i = 0; i < readCount; i++) {
                    String bin = String.format("%8s", Integer.toBinaryString(buffer[i] & 0xFF))
                            .replace(' ', '0');
                    payloadBits.append(bin);
                }
                // Pad final chunk with zeros if needed
                int missing = PAYLOAD_CHARS - readCount;
                for (int i = 0; i < missing; i++) {
                    payloadBits.append("00000000");
                }

                // Build full 480-bit frame string
                String frameBits = senderMacAddress
                        + receiveMacAddress
                        + LEN
                        + payloadBits.toString();


                frames.add(new Frame(frameBits));
            }
        }
        calculateCorrectionMethods(frames);
        return correctedFrames;
    }

    private static void calculateCorrectionMethods(List<Frame> frames) {
        correctedFrames = new ArrayList<>();
        for (int i = 0; i < frames.size(); i++) {
            String frame = frames.get(i).getBits();
            correctedFrames.add(getChecksum(frame));
        }
    }
}
