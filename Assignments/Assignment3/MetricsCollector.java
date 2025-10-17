package Assignments.Assignment3;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.*;

public class MetricsCollector {
    private final AtomicInteger collisions = new AtomicInteger(0);
    private final AtomicInteger attempts   = new AtomicInteger(0);
    private final AtomicLong bitsAttempted = new AtomicLong(0);
    private final AtomicLong bitsSuccessful = new AtomicLong(0);
    private final AtomicLong totalForwardingTimeMs = new AtomicLong(0);
    private final AtomicInteger framesDelivered = new AtomicInteger(0);

    // Record when the simulation starts
    private final long startTimeMs = System.currentTimeMillis();

    public void incrementCollision()      { collisions.incrementAndGet(); }
    public void incrementAttempt()        { attempts.incrementAndGet(); }
    public void addBitsAttempted(long b)  { bitsAttempted.addAndGet(b); }

    public void recordSuccessfulFrame(int stationId, int bits, long delayMs) {
        bitsSuccessful.addAndGet(bits);
        totalForwardingTimeMs.addAndGet(delayMs);
        framesDelivered.incrementAndGet();
    }

    private long getSimTimeMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    public double getThroughputBps() {
        long simTimeMs = getSimTimeMs();
        if (simTimeMs == 0) return 0.0;
        return (bitsSuccessful.get() * 1000.0) / simTimeMs;
    }


    public double getAverageForwardingDelayMs() {
        int delivered = framesDelivered.get();
        return delivered == 0 ? 0.0 : totalForwardingTimeMs.get() * 1.0 / delivered;
    }


    public void exportCsv(String filename) {
        long simTimeMs = getSimTimeMs();
        try (FileWriter fw = new FileWriter(filename)) {
            fw.write("bitsAttempted,bitsSuccessful,collisions,framesDelivered,avgDelayMs,throughputBps,simTimeMs\n");
            fw.write(bitsAttempted.get() + "," +
                    bitsSuccessful.get() + "," +
                    collisions.get() + "," +
                    framesDelivered.get() + "," +
                    getAverageForwardingDelayMs() + "," +
                    getThroughputBps() + "," +
                    simTimeMs + "\n");
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String summary() {
        long simTimeMs = getSimTimeMs();
        return "Metrics:" +
                "\n  collisions           = " + collisions.get() +
                "\n  attempts             = " + attempts.get() +
                "\n  bitsAttempted        = " + bitsAttempted.get() +
                "\n  bitsSuccessful       = " + bitsSuccessful.get() +
                "\n  framesDelivered      = " + framesDelivered.get() +
                "\n  avgForwardingDelayMs = " + getAverageForwardingDelayMs() +
                "\n  throughputBps        = " + getThroughputBps() +
                "\n  simTimeMs            = " + simTimeMs;
    }
}
