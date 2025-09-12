package Assignments.Assignment1;

import java.util.*;

public class ErrorInjector {
    private static Random rand = new Random();
    private static List<List<String >> errorInjectedFrameList;

    // Flip a single bit at position 'pos'
    private static String flipBit(String data, int pos) {
        char[] bits = data.toCharArray();
        bits[pos] = (bits[pos] == '0') ? '1' : '0';
        return new String(bits);
    }

    // Flips k random bits
    public static String flipKBits(String data, int k) {
        int len = 100;

        // Select k distinct positions
        Set<Integer> positions = new HashSet<>();
        while (positions.size() < k) {
            positions.add(rand.nextInt(len));
        }

        // Flip bits at the chosen positions
        char[] bits = data.toCharArray();
        for (int pos : positions) {
            bits[pos] = (bits[pos] == '0') ? '1' : '0';
        }

        return new String(bits);
    }

    // 1. Single-bit error
    public static void singleBitError(List<String> frames,int i) {
        int pos = rand.nextInt(100);
        for(String frame : frames){
            errorInjectedFrameList.get(i).add(flipBit(frame, pos));
        }
    }

    // 2. Two isolated single-bit errors
    public static void twoBitError(List<String> frames,int i) {
        for (String frame : frames) {
            // 50% chance to do random two-bit flips
            if (rand.nextDouble() < 0.20) {
                errorInjectedFrameList.get(i).add(flipKBits(frame,2));
            } else {
                // delegate to your special 2-bit error constructor
                errorInjectedFrameList.get(i).add(twoBitError(frame));
            }
        }
    }

    public static String twoBitError(String frame) {
        if (frame.length() != 512) {
            throw new IllegalArgumentException("Input must be exactly 512 bits");
        }

        final int SEQ_COUNT = 32;  // number of 16-bit sequences in 512 bits
        final int SEQ_LEN = 16;

        // Split into 16-bit chunks
        String[] sequences = new String[SEQ_COUNT];
        for (int i = 0; i < SEQ_COUNT; i++) {
            sequences[i] = frame.substring(i * SEQ_LEN, (i + 1) * SEQ_LEN);
        }

        // Keep track of indices and bit positions to flip
        int flipsDone = 0;

        outer:
        for (int i = 0; i < SEQ_COUNT && flipsDone < 2; i++) {
            for (int j = i + 1; j < SEQ_COUNT && flipsDone < 2; j++) {
                String s1 = sequences[i];
                String s2 = sequences[j];

                // Find first differing bit position
                int differingBit = -1;
                for (int bit = 0; bit < SEQ_LEN; bit++) {
                    if (s1.charAt(bit) != s2.charAt(bit)) {
                        differingBit = bit;
                        break;
                    }
                }

                if (differingBit != -1) {
                    // Flip bit in s1 if we still need flips
                    if (flipsDone < 2) {
                        sequences[i] = flipBit(s1, differingBit);
                        flipsDone++;
                    }
                    // Flip bit in s2 if we still need flips
                    if (flipsDone < 2) {
                        sequences[j] = flipBit(s2, differingBit);
                        flipsDone++;
                    }
                    if (flipsDone >= 2) {
                        break outer;
                    }
                }
            }
        }

        // Recombine sequences into full 512-bit string
        StringBuilder result = new StringBuilder(512);
        for (String seq : sequences) {
            result.append(seq);
        }
        return result.toString();
    }

    // 3. Odd number of errors (e.g., 3 random flips)
    public static void oddErrors(List<String> frames, int i) {
        for(String frame : frames){
            errorInjectedFrameList.get(i).add(flipKBits(frame, 1));
        }
    }

    // 4. Burst error (flip a contiguous block of bits)
    public static void burstError(List<String> frames,int i) {
        for(int j=0;j<frames.size();j++){
            errorInjectedFrameList.get(i).add(CrcErrorInjector.getBurstError(frames.get(j),j));
        }
    }

    // Dispatcher: choose error type by name
    public static List<List<String>> injectError(List<List<String>> frameList) {
        errorInjectedFrameList = new ArrayList<>();
        for (int i = 0; i < frameList.size(); i++) {
            List<String> frames = frameList.get(i);
            errorInjectedFrameList.add(new ArrayList<>());
            int ch = i%5;
            switch(ch){
                case 0:
                    noneType(frames,i);
                    break;
                case 1:
                    singleBitError(frames,i);
                    break;
                case 2:
                    twoBitError(frames,i);
                    break;
                case 3:
                    oddErrors(frames,i);
                    break;
                case 4:
                    burstError(frames,i);
                    break;
            }
        }
        return errorInjectedFrameList;
    }

    private static void noneType(List<String> frames,int i) {
        for(String frame:frames){
            errorInjectedFrameList.get(i).add(frame);
        }
    }
}
