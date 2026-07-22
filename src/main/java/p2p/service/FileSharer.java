package p2p.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import p2p.utils.UploadUtils;

/**
 * Serves a file directly to peers over a raw TCP socket. The file bytes travel
 * socket-to-socket between the two peers with no central relay in the path.
 */
public class FileSharer {

    static final String PROTOCOL = "PEERPASS/1.0";

    private final ConcurrentHashMap<Integer, String> availableFiles;

    public FileSharer() {
        availableFiles = new ConcurrentHashMap<>();
    }

    /** Absolute file path for a given share code (port), or null if not present. */
    public String getFilePath(int port) {
        return availableFiles.get(port);
    }

    /** Registers a file under a dynamically allocated, unique share code (an ephemeral port). */
    public int offerFile(String filePath) {
        while (true) {
            int port = UploadUtils.generateCode();
            String previous = availableFiles.putIfAbsent(port, filePath);
            if (previous == null) {
                return port;
            }
        }
    }

    /**
     * Binds the dynamically allocated port and serves connecting peers until the
     * socket is closed. Each peer gets its own thread, so concurrent transfers run
     * independently with no shared mutable state between them.
     * Blocks the calling thread.
     */
    public void startFileServer(int port) {
        String filePath = availableFiles.get(port);
        if (filePath == null) {
            System.err.println("No file associated with port: " + port);
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serving '" + new File(filePath).getName() + "' on port " + port);
            System.out.println("Peers can fetch it with:  receive " + shareAddress(port));
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Peer connected: " + clientSocket.getInetAddress());
                new Thread(new FileSenderHandler(clientSocket, filePath)).start();
            }
        } catch (IOException e) {
            System.err.println("Error on file server port " + port + ": " + e.getMessage());
        }
    }

    /** Best-effort host:port a peer would dial to reach this sender (LAN address). */
    static String shareAddress(int port) {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            host = "127.0.0.1";
        }
        // ponytail: LAN address only. Across the internet the receiver needs to reach
        // this host:port (port-forwarding); real NAT traversal (STUN/TURN) is out of scope.
        return host + ":" + port;
    }

    private static class FileSenderHandler implements Runnable {
        private final Socket clientSocket;
        private final String filePath;

        FileSenderHandler(Socket clientSocket, String filePath) {
            this.clientSocket = clientSocket;
            this.filePath = filePath;
        }

        @Override
        public void run() {
            try (Socket socket = clientSocket;
                 InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream();
                 FileInputStream fis = new FileInputStream(filePath)) {

                // Handshake: the peer announces itself, we reply with file metadata.
                String hello = readLine(in);
                if (hello == null || !hello.startsWith(PROTOCOL)) {
                    System.err.println("Rejected peer: bad handshake (" + hello + ")");
                    return;
                }

                File f = new File(filePath);
                String header = PROTOCOL + " OK\n" +
                        "Filename: " + f.getName() + "\n" +
                        "Length: " + f.length() + "\n" +
                        "\n";
                out.write(header.getBytes(StandardCharsets.UTF_8));
                out.flush();

                byte[] buffer = new byte[1024 * 1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
                System.out.println("Sent '" + f.getName() + "' to " + socket.getInetAddress());
            } catch (IOException e) {
                System.err.println("Error sending file to peer: " + e.getMessage());
            }
        }
    }

    /** Reads one '\n'-terminated line as UTF-8 without consuming bytes past it. */
    static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                line.write(b);
            }
        }
        if (b == -1 && line.size() == 0) {
            return null;
        }
        return line.toString(StandardCharsets.UTF_8.name());
    }
}
