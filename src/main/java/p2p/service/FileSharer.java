package p2p.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import p2p.utils.UploadUtils;

public class FileSharer {

    private HashMap<Integer, String> availableFiles;

    public FileSharer() {
        availableFiles = new HashMap<>();
    }

    public int offerFile(String filePath) {
        int port;
        while (true) {
            port = UploadUtils.generateCode();
            if (availableFiles.containsKey(port)) {
                continue;
            }
            // Try binding to ensure the port is actually free and reachable
            try (ServerSocket ss = new ServerSocket(port)) {
                ss.setReuseAddress(true);
                availableFiles.put(port, filePath);
                return port;
            } catch (IOException bindErr) {
                // port in use; try another
            }
        }
    }

    public void startFileServer(int port) {
        String filePath = availableFiles.get(port);
        if (filePath == null) {
            System.err.println("No file associated with port: " + port);
            return;
        }

        // Allow multiple receivers to download using the same code.
        // We keep the server socket open and accept clients in a loop.
        // To avoid dangling servers, we enforce a max runtime and optional client limit.
        final int maxClients = 50;          // reasonable upper bound
        final long maxRunMillis = 15 * 60_000L; // 15 minutes
        long start = System.currentTimeMillis();
        int served = 0;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(2_000); // so we can periodically check timeouts

            System.out.println("Serving file '" + new File(filePath).getName() + "' on port " + port +
                               " (multi-client, up to " + maxClients + " clients or " + (maxRunMillis / 60000) + " min)");

            while (served < maxClients && (System.currentTimeMillis() - start) < maxRunMillis) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                    served++;
                    new Thread(new FileSenderHandler(clientSocket, filePath)).start();
                } catch (IOException acceptErr) {
                    // Likely a timeout; loop and re-check limits
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting file server on port " + port + ": " + e.getMessage());
        } finally {
            // Remove mapping so the code can be reused in the future
            availableFiles.remove(port);
            System.out.println("File server on port " + port + " stopped after serving " + served + " client(s).");
        }
    }

    private static class FileSenderHandler implements Runnable {
        private final Socket clientSocket;
        private final String filePath;

        public FileSenderHandler(Socket clientSocket, String filePath) {
            this.clientSocket = clientSocket;
            this.filePath = filePath;
        }

        @Override
        public void run() {
            try (FileInputStream fis = new FileInputStream(filePath);
                 OutputStream oss = clientSocket.getOutputStream()) {

                // Send simple headers: Filename (and total length for info)
                File f = new File(filePath);
                String filename = f.getName();
                long length = f.length();
                String header = "Filename: " + filename + "\n" +
                                "Length: " + length + "\n" +
                                "\n"; // Empty line to end headers
                oss.write(header.getBytes());
                oss.flush(); // Ensure headers are sent immediately
                System.out.println("Sent headers: " + header.trim());

                // Send the file content from start
                byte[] buffer = new byte[1024 * 1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    oss.write(buffer, 0, bytesRead);
                }
                System.out.println("File '" + filename + "' sent to " + clientSocket.getInetAddress());
            } catch (IOException e) {
                System.err.println("Error sending file to client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }

}