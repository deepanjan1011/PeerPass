package p2p.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serving file '" + new File(filePath).getName() + "' on port " + port);
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            new Thread(new FileSenderHandler(clientSocket, filePath)).start();

        } catch (IOException e) {
            System.err.println("Error starting file server on port " + port + ": " + e.getMessage());
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
                 OutputStream oss = clientSocket.getOutputStream();
                 InputStream iss = clientSocket.getInputStream()) {

                // Optional: allow client to send an offset request first
                clientSocket.setSoTimeout(1000); // small timeout to read optional line
                long offset = 0L;
                try {
                    StringBuilder line = new StringBuilder();
                    int ch;
                    while ((ch = iss.read()) != -1) {
                        if (ch == '\n') break;
                        line.append((char) ch);
                        if (line.length() > 64) break; // sanity limit
                    }
                    String maybe = line.toString().trim();
                    if (maybe.startsWith("OFFSET: ")) {
                        try { offset = Long.parseLong(maybe.substring(8).trim()); } catch (NumberFormatException ignore) {}
                    }
                } catch (Exception ignore) {
                    // no offset line; proceed from 0
                } finally {
                    try { clientSocket.setSoTimeout(0); } catch (Exception ignore2) {}
                }

                // Send simple headers: Filename and Length (full length)
                File f = new File(filePath);
                String filename = f.getName();
                long length = f.length();
                String header = "Filename: " + filename + "\n" +
                                "Length: " + length + "\n";
                oss.write(header.getBytes());

                // Position the stream at offset
                if (offset > 0) {
                    long skipped = 0;
                    while (skipped < offset) {
                        long s = fis.skip(offset - skipped);
                        if (s <= 0) break;
                        skipped += s;
                    }
                }

                // Send the file content from offset
                byte[] buffer = new byte[1024 * 1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    oss.write(buffer, 0, bytesRead);
                }
                System.out.println("File '" + filename + "' sent to " + clientSocket.getInetAddress() + (offset > 0 ? (" from offset " + offset) : ""));
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