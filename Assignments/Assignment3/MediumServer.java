package Assignments.Assignment3;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MediumServer {
    private final int port;
    private final int slotMs;
    private final int totalSlots;
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private final MetricsCollector metrics = new MetricsCollector();
    private final double p;

    private volatile ClientHandler currentTransmitter = null;
    private volatile int remainingTxSlots = 0;
    private volatile int transmissionSlotsPerFrame = 0;

    public MediumServer(int port, int slotMs, int totalSlots, double p) throws IOException {
        this.port = port;
        this.slotMs = slotMs;
        this.totalSlots = totalSlots;
        this.p = p; // Store p value for CSV naming

        // Enhanced socket creation to avoid binding issues
        this.serverSocket = new ServerSocket();
        this.serverSocket.setReuseAddress(true); // Allow port reuse

        try {
            this.serverSocket.bind(new java.net.InetSocketAddress(port));
            System.out.println("[Server] Successfully bound to port " + port);
        } catch (java.net.BindException e) {
            System.err.println("[Server] Failed to bind to port " + port + ": " + e.getMessage());
            throw e;
        }
    }

    public void start() {
        System.out.println("[Server] Starting MediumServer on port " + port);
        new Thread(this::acceptClients, "Accept-Thread").start();
        runSlotLoop();
        shutdown();
    }

    private void acceptClients() {
        try {
            while (!serverSocket.isClosed()) {
                Socket s = serverSocket.accept();
                ClientHandler ch = new ClientHandler(s);
                clients.add(ch);
                new Thread(ch, "ClientHandler-" + ch.getId()).start();
                System.out.println("[Server] Accepted client " + ch.getId());
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) e.printStackTrace();
        }
    }

    private void runSlotLoop() {
        int slot = 0;
        final int txWindowMs = Math.max(1, slotMs / 2);

        long simStartTime = System.currentTimeMillis();

        while (slot < totalSlots) {
            slot++;

            String mediumStatus = (currentTransmitter != null) ? "BUSY" : "IDLE";
            broadcast("TICK " + slot + " STATUS " + mediumStatus);

            try { Thread.sleep(txWindowMs); } catch (InterruptedException e) { e.printStackTrace(); }

            List<Message> txs = new ArrayList<>();
            messageQueue.drainTo(txs);
            List<Message> txMsgs = new ArrayList<>();
            for (Message m : txs) if ("TX".equals(m.type)) txMsgs.add(m);

            if (currentTransmitter != null) {
                remainingTxSlots--;
                if (remainingTxSlots <= 0) {
                    long finishTime = System.currentTimeMillis();
                    int bits = currentTransmitter.currentFrameBits;
                    metrics.recordSuccessfulFrame(currentTransmitter.getId(), bits, finishTime - currentTransmitter.frameStartTime);
                    sendToClient(currentTransmitter, "RESULT SUCCESS " + currentTransmitter.currentFrameId + " " + bits + " " + finishTime);
                    System.out.println("[Server] Slot " + slot + ": Transmission by client " + currentTransmitter.getId() + " finished.");
                    currentTransmitter = null;
                    remainingTxSlots = 0;
                }
                if (!txMsgs.isEmpty()) {
                    for (Message m : txMsgs) sendToClient(findHandlerById(m.stationId), "REPLY BUSY");
                }
            } else {
                if (txMsgs.size() == 0) {
                    // nothing
                } else if (txMsgs.size() == 1) {
                    Message m = txMsgs.get(0);
                    ClientHandler ch = findHandlerById(m.stationId);
                    if (ch != null) {
                        currentTransmitter = ch;
                        int bits = m.frameBits;
                        double txTimeSec = ((double) bits) / Utils.BITRATE_BPS;
                        int txMs = Math.max(1, (int)Math.ceil(txTimeSec * 1000.0));
                        transmissionSlotsPerFrame = Math.max(1, (int)Math.ceil((double)txMs / slotMs));
                        remainingTxSlots = transmissionSlotsPerFrame;
                        currentTransmitter.currentFrameId = m.frameId;
                        currentTransmitter.currentFrameBits = bits;
                        currentTransmitter.frameStartTime = System.currentTimeMillis();
                        metrics.incrementAttempt();
                        metrics.addBitsAttempted(bits);
                        System.out.println("[Server] Slot " + slot + ": Client " + ch.getId() + " started TX for " + transmissionSlotsPerFrame + " slots.");
                    }
                } else {
                    metrics.incrementCollision();
                    System.out.println("[Server] Slot " + slot + ": COLLISION among " + txMsgs.size() + " stations.");
                    for (Message m : txMsgs) {
                        ClientHandler ch = findHandlerById(m.stationId);
                        if (ch != null) sendToClient(ch, "RESULT COLLISION");
                    }
                }
            }

            try { Thread.sleep(Math.max(1, slotMs - txWindowMs)); } catch (InterruptedException e) { e.printStackTrace(); }
        }

        // export CSV
        String csvFilename = String.format("Assignments/Assignment3/csma_cd_results_%.1f.csv", p);
        metrics.exportCsv(csvFilename);
        System.out.println("[Server] Results exported to " + csvFilename);
        System.out.println("[Server] " + metrics.summary());
    }

    private void sendToClient(ClientHandler ch, String msg) { if (ch != null) ch.send(msg); }

    private ClientHandler findHandlerById(int id) {
        synchronized (clients) {
            for (ClientHandler ch : clients) if (ch.getId() == id) return ch;
        }
        return null;
    }

    private void broadcast(String msg) { synchronized (clients) { for (ClientHandler ch : clients) ch.send(msg); } }

    public void shutdown() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.close();
                }
                clients.clear();
            }
        } catch (Exception e) {
            System.err.println("[Server] Error during shutdown: " + e.getMessage());
        }
    }

    protected void pushMessage(Message m) { messageQueue.offer(m); }

    class ClientHandler implements Runnable {
        private static int NEXT_ID = 1;
        private final int id;
        private final Socket socket;
        private final BufferedReader in;
        private final BufferedWriter out;
        private volatile boolean running = true;

        // tracking current frame
        public int currentFrameId = -1;
        public int currentFrameBits = 0;
        public long frameStartTime = 0;

        ClientHandler(Socket s) throws IOException {
            this.socket = s;
            this.id = NEXT_ID++;
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            this.out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            send("ASSIGN_ID " + id);
        }

        public int getId() { return id; }
        public void send(String m) { try { out.write(m + "\n"); out.flush(); } catch (IOException e) { running=false; } }
        public void close() { running=false; try { socket.close(); } catch (IOException e) {} }

        @Override
        public void run() {
            try {
                String line;
                while (running && (line=in.readLine())!=null) {
                    line=line.trim(); if(line.isEmpty()) continue;
                    String[] parts = line.split("\\s+");
                    if("TX".equals(parts[0]) && parts.length>=5) {
                        Message m = new Message();
                        m.type="TX";
                        m.stationId=Integer.parseInt(parts[1]);
                        m.frameId=Integer.parseInt(parts[2]);
                        m.enqueueTimeMs=Long.parseLong(parts[3]);
                        m.frameBits=Integer.parseInt(parts[4]);
                        pushMessage(m);
                    }
                }
            } catch(IOException e){} finally { running=false; try{socket.close();}catch(IOException e){} }
        }
    }

    static class Message {
        String type;
        int stationId;
        int frameId;
        long enqueueTimeMs;
        int frameBits;
    }
}
