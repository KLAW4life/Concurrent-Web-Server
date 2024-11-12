import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentWebServer {
    private static final int PORT = 8080;
    private static final AtomicInteger clientCounter = new AtomicInteger(0); // Tracks client count

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                // Accept incoming client connections
                Socket clientSocket = serverSocket.accept();

                // Increment client count for a unique message
                int clientNumber = clientCounter.incrementAndGet();
                System.out.println("New client connected, assigned client number: " + clientNumber);

                // Handle client in a new thread
                new Thread(new ClientHandler(clientSocket, clientNumber)).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private int clientNumber;

    public ClientHandler(Socket socket, int clientNumber) {
        this.clientSocket = socket;
        this.clientNumber = clientNumber;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Read HTTP request
            String line = in.readLine();
            if (line != null && line.startsWith("GET")) {
                // Send 200 OK response with unique hello message
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/plain");
                out.println("Connection: close");
                out.println();
                out.println("Hello to client " + clientNumber + "!");
                System.out.println("Responded with 200 OK to client " + clientNumber);
            } else {
                // Send 400 Bad Request response for unsupported requests
                out.println("HTTP/1.1 400 Bad Request");
                out.println("Content-Type: text/plain");
                out.println("Connection: close");
                out.println();
                out.println("400 Bad Request - This server only supports GET requests.");
                System.out.println("Responded with 400 Bad Request to client " + clientNumber);
            }

        } catch (IOException e) {
            System.err.println("Client handler exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Could not close client socket: " + e.getMessage());
            }
        }
    }
}
