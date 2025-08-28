package Assignments.Assignment1;

/**
 * Checksum Calculator and Error Injection Utility
 * Input/output as binary strings. Handles frames up to 60 bytes (480 bits),
 * pads with zeros to 480 bits, then appends 16 bits zero padding and 16-bit checksum
 * to produce a 512-bit binary string.
 */
public class ChecksumErrorInjector {

    private static final int INPUT_BITS = 480;   // 60 bytes * 8
    private static final int OUTPUT_BITS = 512;  // 64 bytes * 8

    /**
     * Convert binary string to byte array.
     * Pads input to 480 bits with zeros if shorter.
     * @param bin binary string (max 480 chars)
     * @return 60-byte array
     */
    private static byte[] binaryStringToBytes(String bin) {
        if (bin.length() > INPUT_BITS) {
            throw new IllegalArgumentException("Input binary string length must be ≤ 480 bits");
        }
        // Pad to 480 bits
        StringBuilder sb = new StringBuilder(bin);
        while (sb.length() < INPUT_BITS) {
            sb.append('0');
        }
        String padded = sb.toString();
        byte[] data = new byte[INPUT_BITS / 8];
        for (int i = 0; i < data.length; i++) {
            int idx = i * 8;
            data[i] = (byte) Integer.parseInt(padded.substring(idx, idx + 8), 2);
        }
        return data;
    }

    /**
     * Convert byte array to binary string.
     * @param data byte array
     * @return binary string
     */
    private static String bytesToBinaryString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 8);
        for (byte b : data) {
            sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return sb.toString();
    }

    /**
     * Calculate 16-bit checksum for given data.
     * @param data byte array
     * @return 16-bit checksum
     */
    private static int calculateChecksum16(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += (b & 0xFF);
        }
        return sum & 0xFFFF;
    }

    /**
     * Build 64-byte frame from 60-byte data array.
     * Appends 16 bits zero padding then 16-bit checksum.
     * @param frame60 60-byte array
     * @return 64-byte array
     */
    private static byte[] buildFrame(byte[] frame60) {
        byte[] result = new byte[64];
        System.arraycopy(frame60, 0, result, 0, 60);
        // bytes 60-61 = zero
        result[60] = 0;
        result[61] = 0;
        int checksum = calculateChecksum16(frame60);
        result[62] = (byte) ((checksum >> 8) & 0xFF);
        result[63] = (byte) (checksum & 0xFF);
        return result;
    }

    /**
     * Two isolated single-bit errors that cancel each other out.
     * @param inputBin binary string (≤480 bits)
     * @return 512-bit binary string with errors and checksum
     */
    public static String twoIsolatedSingleBitErrors(String inputBin) {
        byte[] frame = binaryStringToBytes(inputBin);
        // Error 1: flip bit at overall bit index 10*8 (byte 10, bit 0)
        frame[10] ^= (byte) 0x80;
        // Error 2: flip bit at overall bit index 30*8 (byte 30, bit 0)
        frame[30] ^= (byte) 0x80;
        // Then build frame and return binary string
        return bytesToBinaryString(buildFrame(frame));
    }

    /**
     * Odd number of errors (3) that sum to zero.
     * @param inputBin binary string (≤480 bits)
     * @return 512-bit binary string with errors and checksum
     */
    public static String oddNumberOfErrors(String inputBin) {
        byte[] frame = binaryStringToBytes(inputBin);
        // +85 at byte 5
        frame[5] = (byte) ((frame[5] & 0xFF) + 85);
        // +65 at byte 15
        frame[15] = (byte) ((frame[15] & 0xFF) + 65);
        // -150 at byte 25
        frame[25] = (byte) ((frame[25] & 0xFF) - 150);
        return bytesToBinaryString(buildFrame(frame));
    }

    /**
     * Burst errors that cancel each other out.
     * @param inputBin binary string (≤480 bits)
     * @return 512-bit binary string with errors and checksum
     */
    public static String burstErrors(String inputBin) {
        byte[] frame = binaryStringToBytes(inputBin);
        // Burst 1: bytes 8-11
        frame[8]  = (byte) ((frame[8]  & 0xFF) + 45);
        frame[9]  = (byte) ((frame[9]  & 0xFF) + 35);
        frame[10] = (byte) ((frame[10] & 0xFF) + 30);
        frame[11] = (byte) ((frame[11] & 0xFF) + 25);
        // Burst 2: bytes 20-23
        frame[20] = (byte) ((frame[20] & 0xFF) - 35);
        frame[21] = (byte) ((frame[21] & 0xFF) - 30);
        frame[22] = (byte) ((frame[22] & 0xFF) - 35);
        frame[23] = (byte) ((frame[23] & 0xFF) - 35);
        return bytesToBinaryString(buildFrame(frame));
    }

    /**
     * Clean frame with correct checksum (no errors).
     * @param inputBin binary string (≤480 bits)
     * @return 512-bit binary string with checksum
     */
    public static String cleanFrameWithChecksum(String inputBin) {
        byte[] frame = binaryStringToBytes(inputBin);
        return bytesToBinaryString(buildFrame(frame));
    }
}
