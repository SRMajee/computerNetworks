package Assignments.Assignment1;

import java.util.*;

/**
 * Error injection utilities.
 * <p>
 * - checksum: invert all bits
 * - i <= degree: random unique flips (detectable)
 * - i > degree: construct error vector that is a multiple of generator polynomial g(x)
 * by XORing shifted copies of g(x) -> undetected by the CRC for that poly
 */
public class CrcErrorInjector {

    private static final Random rand = new Random();

    private static class CRCParams {
        final int degree;
        final int poly; // lower-degree bits (same representation you used in computeCrc)

        CRCParams(int degree, int poly) {
            this.degree = degree;
            this.poly = poly;
        }
    }

    // Return CRC params or null for checksum
    private static CRCParams getCRCParams(int scheme) {
        switch (scheme) {
            case 0:
                return null;
            case 1:
                return new CRCParams(8, 0x07);
            case 2:
                return new CRCParams(10, 0x233);
            case 3:
                return new CRCParams(16, 0x1021);
            case 4:
                return new CRCParams(32, 0x04C11DB7);
            default:
                throw new IllegalArgumentException("Unknown scheme: " + scheme);
        }
    }

    // Flip bits in data in range [start, start+len)
    private static String flipBits(String data, int start, int len) {
        if (start < 0 || start + len > data.length()) {
            throw new IllegalArgumentException("flipBits out of range");
        }
        char[] chars = data.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = start + i;
            chars[pos] = (chars[pos] == '0') ? '1' : '0';
        }
        return new String(chars);
    }

    // Randomly flip `flips` distinct bits (if flips >= length => flip all bits)
    private static String randomFlipBits(String data, int flips) {
        char[] chars = data.toCharArray();
        int length = chars.length;
        if (flips >= length) {
            for (int i = 0; i < length; i++) chars[i] = (chars[i] == '0') ? '1' : '0';
            return new String(chars);
        }
        List<Integer> indices = new ArrayList<>(length);
        for (int i = 0; i < length; i++) indices.add(i);
        Collections.shuffle(indices, rand);
        for (int j = 0; j < flips; j++) {
            int pos = indices.get(j);
            chars[pos] = (chars[pos] == '0') ? '1' : '0';
        }
        return new String(chars);
    }

    /**
     * Inject error burst in data according to scheme:
     * - checksum: flip all bits
     * - if i <= degree: random unique flips
     * - if i > degree: construct undetectable error (multiple of g(x)) by XORing shifted copies of g(x)
     *
     * @param data   binary string (e.g., 512 bits)
     * @param scheme "crc8","crc10","crc16","crc32","checksum"
     * @return mutated binary string with errors injected
     */
    public static String getBurstError(String data, int scheme) {
        CRCParams params = getCRCParams(scheme);
        int length = data.length();

        // Decide burst length i
        int i;
        if (params == null) { // checksum
            // user requested "flip all bits" -> just invert everything
            char[] arr = data.toCharArray();
            for (int k = 0; k < arr.length; k++) arr[k] = (arr[k] == '0') ? '1' : '0';
            return new String(arr);
        } else {
            int degree = params.degree;
            i = rand.nextInt(degree * 5) + 1; // may be <= or > degree
            if (i > length) i = length;
            if (i <= degree) {
                return randomFlipBits(data, i);
            } else {
                return createUndetectedError(data, params.degree, params.poly, i);
            }
        }
    }

    /**
     * Create an error vector E that is a multiple of g(x) so the CRC will not detect it.
     * We do this by XORing (adding mod-2) several shifted copies of g_full, where
     * g_full = x^degree + (poly bits), and degree = n.
     * <p>
     * The number of shifts is chosen so that the span (burst length) of the resulting
     * error is at least targetBurst. We choose overlapping shifts by 1 position to
     * make the final error contiguous (or near-contiguous) and ensure span >= targetBurst.
     */
    private static String createUndetectedError(String data, int degree, int poly, int targetBurst) {
        final int dataLen = data.length();
        final int gLen = degree + 1; // g_full has gLen bits
        // Build g_full as a long: high bit is x^degree, low bits are 'poly' (degree bits)
        long gFull = ((1L << degree) | (((long) poly) & ((1L << degree) - 1L)));

        // Number of shifted copies required to make span >= targetBurst
        int numShifts = Math.max(1, targetBurst - gLen + 1);

        // If cannot fit with that many shifts, reduce numShifts so shifts fit in frame
        if (gLen + (numShifts - 1) > dataLen) {
            // max contiguous span we can obtain by overlapping shifts of gFull
            numShifts = Math.max(1, dataLen - gLen + 1);
        }

        // Choose start so that last shifted copy fits inside data
        int maxStart = dataLen - gLen - (numShifts - 1);
        if (maxStart < 0) maxStart = 0;
        int start = rand.nextInt(maxStart + 1);

        // Build error boolean array by XORing shifted copies
        boolean[] err = new boolean[dataLen];

        for (int s = 0; s < numShifts; s++) {
            int shiftPos = start + s;
            for (int j = 0; j < gLen; j++) {
                int pos = shiftPos + j;
                if (pos >= dataLen) break; // safety
                // bit j of gFull when j counts MSB->LSB:
                // MSB index in gFull is (gLen-1), so extract (gFull >> (gLen-1-j))
                int gbit = (int) ((gFull >> (gLen - 1 - j)) & 1L);
                if (gbit == 1) err[pos] = !err[pos]; // XOR in
            }
        }

        // Apply error bits to data
        char[] chars = data.toCharArray();
        for (int k = 0; k < dataLen; k++) {
            if (err[k]) chars[k] = (chars[k] == '0') ? '1' : '0';
        }
        return new String(chars);
    }
}
