package Assignments.Assignment3;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class StationClient {
    private final String host;
    private final int port;
    private int stationId = -1;
    private final int clientIdProvided;
    private final double p;
    private final int framesToSend;
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private final Queue<Frame> queue = new ConcurrentLinkedQueue<>();
    private final Random rng = new Random();
    private int backoffSlots = 0;
    private int collisionCountForFrame = 0;
    private Frame currentFrame = null;

    public StationClient(String host, int port, int clientIdProvided, double p, int framesToSend) {
        this.host = host;
        this.port = port;
        this.clientIdProvided = clientIdProvided;
        this.p = p;
        this.framesToSend = framesToSend;
        prepareFrames();
    }

    private void prepareFrames() {
        for (int i = 1; i <= framesToSend; i++) queue.add(new Frame(i, Utils.FRAME_SIZE_BYTES * 8));
    }

    public void start() throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        new Thread(this::readerLoop, "Reader-" + clientIdProvided).start();
    }

    private void readerLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if ("ASSIGN_ID".equals(parts[0])) {
                    stationId = Integer.parseInt(parts[1]);
                    System.out.println("[Station " + clientIdProvided + "] Assigned id " + stationId);
                } else if ("TICK".equals(parts[0]) && parts.length >= 4 && "STATUS".equals(parts[2]))
                    handleTick(parts[3]);
                else if ("RESULT".equals(parts[0])) handleResult(parts[1], parts);
                else if ("REPLY".equals(parts[0]) && parts.length >= 2 && "BUSY".equals(parts[1])) {
                } // ignore
            }
        } catch (IOException e) {
        }
    }

    private void handleTick(String status) {
        if (backoffSlots > 0) {
            backoffSlots--;
            return;
        }
        if (currentFrame == null) currentFrame = queue.peek();
        if (currentFrame == null) return;
        if ("IDLE".equals(status)) {
            if (rng.nextDouble() <= p) {
                long now = System.currentTimeMillis();
                sendRaw("TX " + stationId + " " + currentFrame.id + " " + now + " " + currentFrame.bits);
            }
        }
    }

    private void handleResult(String res, String[] parts) {
        if ("SUCCESS".equals(res) && parts.length >= 5) {
            int frameId = Integer.parseInt(parts[2]);
            int bits = Integer.parseInt(parts[3]);
            long finishTime = Long.parseLong(parts[4]);
            if (currentFrame != null && currentFrame.id == frameId) {
                long delay = finishTime - currentFrame.enqueueTime;
                System.out.println("[Station " + stationId + "] Frame " + frameId + " delivered. Delay(ms)=" + delay);
                queue.poll();
                currentFrame = null;
                collisionCountForFrame = 0;
                backoffSlots = 0;
            }
        } else if ("COLLISION".equals(res)) {
            collisionCountForFrame++;
            int k = Math.min(collisionCountForFrame, Utils.MAX_BACKOFF_EXP);
            backoffSlots = rng.nextInt(1 << k);
            System.out.println("[Station " + stationId + "] Collision on frame " + (currentFrame == null ? "?" : currentFrame.id) + ". Backoff slots=" + backoffSlots);
        }
    }

    private void sendRaw(String s) {
        try {
            out.write(s + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
