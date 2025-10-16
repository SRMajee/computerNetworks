package Assignments.Assignment3;

public class Main {
    public static void main(String[] args) throws Exception {
        // Parse command line arguments (keeping same structure as previous assignments)
        int numStations = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        double p = args.length > 1 ? Double.parseDouble(args[1]) : 0.5;
        int framesPerStation = args.length > 2 ? Integer.parseInt(args[2]) : 50;
        int slotTimeMs = args.length > 3 ? Integer.parseInt(args[3]) : 10;
        int simSlots = args.length > 4 ? Integer.parseInt(args[4]) : 1000;
        int port = args.length > 5 ? Integer.parseInt(args[5]) : 5000;

        System.out.printf("=== P-PERSISTENT CSMA/CD SIMULATION (p=%.1f) ===%n", p);
        System.out.printf("Stations: %d, Port: %d, Frames per station: %d%n",
                numStations, port, framesPerStation);
        System.out.printf("Slot time: %d ms, Total slots: %d%n", slotTimeMs, simSlots);

        // Start server in separate thread (like receiver in previous assignments)
        Thread serverThread = new Thread(() -> {
            try {
                MediumServer server = new MediumServer(port, slotTimeMs, simSlots, p);
                server.start();
            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
                e.printStackTrace();
            }
        }, "MediumServer");

        serverThread.start();
        Thread.sleep(200); // Allow server to start (like previous assignments)

        // Start station clients (like sender in previous assignments)
        for (int i = 1; i <= numStations; i++) {
            final int stationId = i;
            new Thread(() -> {
                try {
                    StationClient client = new StationClient("localhost", port, stationId, p, framesPerStation);
                    client.start();
                } catch (Exception e) {
                    System.err.println("Station " + stationId + " error: " + e.getMessage());
                }
            }, "Station-" + i).start();
            Thread.sleep(50); // Stagger connections
        }

        // Wait for server to complete (like waiting for receiver)
        serverThread.join();

        System.out.printf("Simulation with p=%.1f completed successfully%n", p);
    }
}