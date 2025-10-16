package Assignments.Assignment3;

public class Frame {
    public final int id;
    public final int bits;
    public final long enqueueTime;

    public Frame(int id, int bits) {
        this.id = id;
        this.bits = bits;
        this.enqueueTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.format("Frame{id=%d, bits=%d, enqueueTime=%d}",
                id, bits, enqueueTime);
    }
}