import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentWebServer {
    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10; // Adjust based on server capability
    private static final AtomicInteger clientCounter = new AtomicInteger(0);

    public static void main(String[] args) {
        // Create a fixed thread pool
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                // Accept incoming client connections
                Socket clientSocket = serverSocket.accept();

                // Increment client count for a unique message
                int clientNumber = clientCounter.incrementAndGet();
                System.out.println("New client connected, assigned client number: " + clientNumber);

                // Submit client handler to the thread pool
                threadPool.submit(new ClientHandler(clientSocket, clientNumber));
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Shutdown thread pool gracefully when server stops
            threadPool.shutdown();
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final int clientNumber;

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
            if (line != null) {
                if (line.startsWith("GET")) {
                    handleGetRequest(out);
                } else if (line.startsWith("POST")) {
                    handlePostRequest(in, out);
                } else {
                    sendBadRequestResponse(out);
                }
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

    private void handleGetRequest(PrintWriter out) {
        // Send 200 OK response with unique hello message
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/plain");
        out.println("Connection: close");
        out.println();
        out.println("Hello to client " + clientNumber + "!");
        System.out.println("Responded with 200 OK to client " + clientNumber);
    }

    private void handlePostRequest(BufferedReader in, PrintWriter out) throws IOException {
        // Read headers to determine content length if available
        String line;
        int contentLength = 0;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
            }
        }

        // Read the POST data based on the content length
        char[] postData = new char[contentLength];
        in.read(postData, 0, contentLength);

        // Send a 200 OK response with the received data
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/plain");
        out.println("Connection: close");
        out.println();
        out.println("Received POST data: " + new String(postData));
        System.out.println("Responded with 200 OK to client " + clientNumber + " with POST data: " + new String(postData));
    }

    private void sendBadRequestResponse(PrintWriter out) {
        // Send 400 Bad Request response for unsupported requests
        out.println("HTTP/1.1 400 Bad Request");
        out.println("Content-Type: text/plain");
        out.println("Connection: close");
        out.println();
        out.println("400 Bad Request - This server only supports GET and POST requests.");
        System.out.println("Responded with 400 Bad Request to client " + clientNumber);
    }
}
