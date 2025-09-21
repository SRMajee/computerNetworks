package socketProgramming;

import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        int port = 5000; // server port
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Waiting for client...");

            try (Socket socket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                System.out.println("Client connected.");
                String message = in.readLine(); // read message from client
                System.out.println("Client says: " + message);

                out.println("Hello from Server!"); // send reply to client
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }
}
