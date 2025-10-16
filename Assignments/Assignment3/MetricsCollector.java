package Assignments.Assignment3;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.*;

public class MetricsCollector {
    private final AtomicInteger collisions=new AtomicInteger(0);
    private final AtomicInteger attempts=new AtomicInteger(0);
    private final AtomicLong bitsAttempted=new AtomicLong(0);
    private final AtomicLong bitsSuccessful=new AtomicLong(0);
    private final AtomicLong totalForwardingTimeMs=new AtomicLong(0);
    private final AtomicInteger framesDelivered=new AtomicInteger(0);

    public void incrementCollision(){ collisions.incrementAndGet(); }
    public void incrementAttempt(){ attempts.incrementAndGet(); }
    public void addBitsAttempted(long b){ bitsAttempted.addAndGet(b); }

    public void recordSuccessfulFrame(int stationId,int bits,long delayMs){
        bitsSuccessful.addAndGet(bits);
        totalForwardingTimeMs.addAndGet(delayMs);
        framesDelivered.incrementAndGet();
    }

    public double getThroughputBps(long simTimeMs){ return (bitsSuccessful.get()*1000.0)/simTimeMs; }
    public double getAverageForwardingDelayMs(){ return (framesDelivered.get()==0)?0:totalForwardingTimeMs.get()*1.0/framesDelivered.get(); }

    public void exportCsv(String filename){
        try(FileWriter fw=new FileWriter(filename)){
            fw.write("bitsAttempted,bitsSuccessful,collisions,framesDelivered,avgDelayMs,throughputBps\n");
            fw.write(bitsAttempted.get()+","+bitsSuccessful.get()+","+collisions.get()+","+
                    framesDelivered.get()+","+getAverageForwardingDelayMs()+","+getThroughputBps(1000)+"\n");
            fw.flush();
        }catch(IOException e){ e.printStackTrace(); }
    }

    public String summary(){
        return "Metrics:\n  collisions="+collisions.get()+
                "\n  attempts="+attempts.get()+
                "\n  bitsAttempted="+bitsAttempted.get()+
                "\n  bitsSuccessful="+bitsSuccessful.get()+
                "\n  framesDelivered="+framesDelivered.get()+
                "\n  avgForwardingDelay(ms)="+getAverageForwardingDelayMs()+
                "\n  throughput(bps)="+getThroughputBps(1000);
    }
}
