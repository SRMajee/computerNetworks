package Assignments.Assignment1;

import static Assignments.Assignment1.Receiver.detectedFrames;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class Utils {
    /**
     * Computes a 16-bit checksum by summing each 8-bit byte modulo 2^16.
     * Appends the 16-bit checksum to the 480-bit frame, then pads with zeros
     * up to 512 bits (64 bytes).
     *
     * @param frame 480-bit binary string
     * @return 512-bit binary string: frame + 16-bit checksum + zero padding
     */
    public static String getChecksum(String frame) {
        final int FRAME_BITS = 480;
        final int TARGET_BITS = 512;
        int sum = 0;

        // Sum as 16-bit words (30 words = 60 bytes)
        for (int i = 0; i < FRAME_BITS; i += 16) {
            int word = Integer.parseInt(frame.substring(i, i + 16), 2);
            sum += word;
            // Wrap around carry (ones' complement addition)
            if ((sum & 0x10000) != 0) {
                sum = (sum & 0xFFFF) + 1;
            }
        }

        // One's complement of the sum
        int checksum = ~sum & 0xFFFF;

        // Binary string
        String chk = String.format("%16s", Integer.toBinaryString(checksum))
                .replace(' ', '0');

        // Append checksum and pad to 512 bits
        StringBuilder out = new StringBuilder(frame).append(chk);
        int padding = TARGET_BITS - out.length();
        for (int i = 0; i < padding; i++) {
            out.append('0');
        }
        return out.toString();
    }

    /**
     * Computes an N-bit CRC for a 60-byte (480-bit) frame, appends the CRC bits,
     * and pads with zeros to a total of 64 bytes (512 bits).
     *
     * @param frameBits 480-character string of '0'/'1' (60 bytes)
     * @param width     CRC width in bits: one of 8, 10, 16, or 32
     * @return 512-character string: original 480 bits + CRC width bits + zero
     *         padding
     */
    public static String getCrc(String frameBits, int width) {
        if (frameBits.length() != 480) {
            throw new IllegalArgumentException("Frame must be exactly 480 bits");
        }
        // Convert the 480-bit string into a 60-byte array
        byte[] data = new byte[60];
        for (int i = 0; i < 60; i++) {
            int off = i * 8;
            data[i] = (byte) Integer.parseInt(frameBits.substring(off, off + 8), 2);
        }

        // Select CRC parameters by width
        int poly, init, xorout;
        boolean refin, refout;
        switch (width) {
            case 8:
                poly = 0x07;
                init = 0x00;
                xorout = 0x00;
                refin = false;
                refout = false;
                break;
            case 10:
                poly = 0x233;
                init = 0x000;
                xorout = 0x000;
                refin = false;
                refout = false;
                break;
            case 16:
                poly = 0x1021;
                init = 0xFFFF;
                xorout = 0x0000;
                refin = false;
                refout = false;
                break;
            case 32:
                poly = 0x04C11DB7;
                init = 0xFFFFFFFF;
                xorout = 0xFFFFFFFF;
                refin = true;
                refout = true;
                break;
            default:
                throw new IllegalArgumentException("Unsupported CRC width: " + width);
        }

        // Compute the CRC value
        int crcValue = computeCrc(data, width, poly, init, refin, refout, xorout);

        // Convert CRC to zero-padded binary string of length 'width'
        long mask = (width == 32) ? 0xFFFFFFFFL : ((1L << width) - 1);
        String crcBits = String.format("%" + width + "s",
                Long.toBinaryString(((long) crcValue) & mask))
                .replace(' ', '0');

        // Append CRC bits and pad zeros to reach 512 bits
        StringBuilder sb = new StringBuilder(frameBits).append(crcBits);
        int padding = 512 - sb.length();
        for (int i = 0; i < padding; i++) {
            sb.append('0');
        }
        return sb.toString();
    }

    /**
     * Generic CRC computation for arbitrary width.
     *
     * @param data   byte array over which to compute CRC
     * @param width  CRC width in bits
     * @param poly   generator polynomial
     * @param init   initial remainder value
     * @param refin  reflect input bytes
     * @param refout reflect remainder before final xor
     * @param xorout final xor value
     * @return CRC value masked to 'width' bits
     */
    public static int computeCrc(
            byte[] data,
            int width,
            int poly,
            int init,
            boolean refin,
            boolean refout,
            int xorout) {
        int topBit = 1 << (width - 1);
        int mask = (width == 32) ? 0xFFFFFFFF : ((1 << width) - 1);
        int crc = init & mask;

        for (byte b : data) {
            int curr = b & 0xFF;
            if (refin) {
                curr = Integer.reverse(curr) >>> 24;
            }
            crc ^= (curr << (width - 8)) & mask;
            for (int i = 0; i < 8; i++) {
                if ((crc & topBit) != 0) {
                    crc = ((crc << 1) ^ poly) & mask;
                } else {
                    crc = (crc << 1) & mask;
                }
            }
        }

        if (refout) {
            crc = Integer.reverse(crc) >>> (32 - width);
        }
        return (crc ^ xorout) & mask;
    }

    protected static Integer validateCrc(String frame, int width) {
        try {
            // Extract the original 480-bit data (first 480 bits)
            String originalData = frame.substring(0, 480);

            // Extract the received CRC (next 'width' bits after the 480-bit data)
            String receivedCrc = frame.substring(480, 480 + width);

            // Recompute the CRC for the original data
            String recomputedFrame = getCrc(originalData, width);
            String recomputedCrc = recomputedFrame.substring(480, 480 + width);

            // Compare received CRC with recomputed CRC
            if (receivedCrc.equals(recomputedCrc)) {
                return 0; // No error detected
            } else {
                return 1; // Error detected
            }

        } catch (Exception e) {
            System.err.println("Error validating CRC" + width + ": " + e.getMessage());
            return 0; // Assume no error if validation fails
        }
    }

    protected static Integer validateCheckSum(String frame) {
        try {
            // Extract the original 480-bit data (first 480 bits)
            String originalData = frame.substring(0, 480);

            // Extract the received checksum (next 16 bits after the 480-bit data)
            String receivedChecksum = frame.substring(480, 496);

            // Recompute the checksum for the original data
            String recomputedFrame = getChecksum(originalData);
            String recomputedChecksum = recomputedFrame.substring(480, 496);

            // Compare received checksum with recomputed checksum
            if (receivedChecksum.equals(recomputedChecksum)) {
                return 0; // No error detected
            } else {
                return 1; // Error detected
            }

        } catch (Exception e) {
            System.err.println("Error validating checksum: " + e.getMessage());
            return 0; // Assume no error if validation fails
        }
    }

    // 1) Write detectedFrames to CSV
    protected static void exportDetectedFramesCsv(String csvPath) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(csvPath))) {
            // Header
            pw.println("frame,errorType,checksum,crc8,crc10,crc16,crc32");
            // Rows
            for (int i = 0; i < detectedFrames.size(); i++) {
                List<Integer> row = detectedFrames.get(i);
                if (row.size() < 6)
                    continue;
                pw.printf("%d,%d,%d,%d,%d,%d,%d%n",
                        i,
                        row.get(0),
                        row.get(1),
                        row.get(2),
                        row.get(3),
                        row.get(4),
                        row.get(5));
            }
        }
    }


    protected static void prettyDisplay() {
        // Print results beautifully
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                        ERROR DETECTION RESULTS");
        System.out.println("=".repeat(80));

        // Print header
        System.out.printf("%-8s %-12s %-10s %-8s %-8s %-8s %-8s%n",
                "Frame", "Error Type", "Checksum", "CRC8", "CRC10", "CRC16", "CRC32");
        System.out.println("-".repeat(80));

        // Error type names for display
        String[] errorTypeNames = { "None", "Single", "Two", "Odd", "Burst" };

        for (int i = 0; i < detectedFrames.size(); i++) {
            List<Integer> frameResult = detectedFrames.get(i);

            if (frameResult.size() >= 6) {
                int errorType = frameResult.get(0);
                String errorTypeName = (errorType >= 0 && errorType < errorTypeNames.length) ? errorTypeNames[errorType]
                        : "Unknown";

                System.out.printf("%-8d %-12s %-10s %-8s %-8s %-8s %-8s%n",
                        i,
                        errorTypeName,
                        (frameResult.get(1) == 1) ? "DETECTED" : "NO ERROR",
                        frameResult.get(2) == 1 ? "DETECTED" : "NO ERROR",
                        frameResult.get(3) == 1 ? "DETECTED" : "NO ERROR",
                        frameResult.get(4) == 1 ? "DETECTED" : "NO ERROR",
                        frameResult.get(5) == 1 ? "DETECTED" : "NO ERROR");
            }
        }

        System.out.println("-".repeat(80));

        // Print summary statistics
        System.out.println("\n                           DETECTION SUMMARY");
        System.out.println("=".repeat(80));

        // Count detection rates for each scheme
        int[] totalByErrorType = new int[5]; // Count of each error type
        int[][] detectionCount = new int[5][5]; // [errorType][scheme] detection count

        for (int i = 0; i < detectedFrames.size(); i++) {
            List<Integer> frameResult = detectedFrames.get(i);
            if (frameResult.size() >= 6) {
                int errorType = frameResult.get(0);
                if (errorType >= 0 && errorType < 5) {
                    totalByErrorType[errorType]++;
                    for (int scheme = 0; scheme < 5; scheme++) {
                        if (frameResult.get(scheme + 1) == 1) {
                            detectionCount[errorType][scheme]++;
                        }
                    }
                }
            }
        }

        // Print detection rates
        System.out.printf("%-12s %-10s %-8s %-8s %-8s %-8s%n",
                "Error Type", "Checksum", "CRC8", "CRC10", "CRC16", "CRC32");
        System.out.println("-".repeat(80));

        for (int errorType = 0; errorType < 5; errorType++) {
            if (totalByErrorType[errorType] > 0) {
                System.out.printf("%-12s ", errorTypeNames[errorType]);
                for (int scheme = 0; scheme < 5; scheme++) {
                    double rate = (double) detectionCount[errorType][scheme] / totalByErrorType[errorType] * 100;
                    System.out.printf("%-8s ", String.format("%.0f%%", rate));
                }
                System.out.println();
            }
        }

        System.out.println("=".repeat(80));

    }

}
