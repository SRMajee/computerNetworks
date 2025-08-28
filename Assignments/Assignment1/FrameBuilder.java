package Assignments.Assignment1;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static Assignments.Assignment1.Utils.getChecksum;
import static Assignments.Assignment1.Utils.getCrc;

public class FrameBuilder {

    /**
     * Represents a 60-byte frame (480 bits) as a binary string.
     */
    public static class Frame {
        private final String bits;    // 480-character string of '0'/'1'

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
    static List<List<String>> frameList;

    /**
     * Reads a text file in 46-char blocks, converts to 8-bit-per-char binary,
     * prepends sender, receiver, and LEN fields, and returns a list of Frames.
     *
     * @param inputFilePath     Path to the text file to read.
     * @param senderMacAddress  6-byte (48-bit) binary string for sender MAC.
     * @param receiveMacAddress 6-byte (48-bit) binary string for receiver MAC.
     * @param LEN               2-byte (16-bit) binary string for payload length.
     * @return List of Frame objects, each containing exactly 480 bits.
     * @throws IOException If file reading fails.
     */
    public static List<List<String>> createFrames(
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

                // Sanity check
                if (frameBits.length() != FRAME_BITS) {
                    throw new IllegalStateException(
                            "Frame length is " + frameBits.length() + " bits, expected " + FRAME_BITS
                    );
                }

                frames.add(new Frame(frameBits));
            }
        }

        calculateCorrectionMethods(frames);
        return ErrorInjector.injectError(frameList);

    }

    private static void calculateCorrectionMethods(List<Frame> frames) {
        frameList = new ArrayList<>();
        for (int i = 0; i < frames.size(); i++) {
            frameList.add(new ArrayList<>());
            String frame = frames.get(i).getBits();
            frameList.get(i).add(getChecksum(frame));
            frameList.get(i).add(getCrc(frame, 8));
            frameList.get(i).add(getCrc(frame, 10));
            frameList.get(i).add(getCrc(frame, 16));
            frameList.get(i).add(getCrc(frame, 32));
        }
    }


}
