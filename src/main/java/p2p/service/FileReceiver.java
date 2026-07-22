package p2p.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * The other half of the peer: connects directly to a sender's socket, performs
 * the text handshake, and streams the file to disk. No central relay involved.
 */
public class FileReceiver {

    /**
     * Connects to host:port, downloads the offered file into outDir, and returns
     * the saved file. Verifies the received byte count against the advertised length.
     */
    public static File receive(String host, int port, String outDir) throws IOException {
        try (Socket socket = new Socket(host, port);
             InputStream in = new BufferedInputStream(socket.getInputStream());
             OutputStream sockOut = socket.getOutputStream()) {

            // Handshake: announce ourselves, then read the sender's reply headers.
            sockOut.write((FileSharer.PROTOCOL + " HELLO\n").getBytes(StandardCharsets.UTF_8));
            sockOut.flush();

            String status = FileSharer.readLine(in);
            if (status == null || !status.startsWith(FileSharer.PROTOCOL + " OK")) {
                throw new IOException("Unexpected handshake response: " + status);
            }

            String filename = "download.bin";
            long expected = -1;
            String line;
            while ((line = FileSharer.readLine(in)) != null && !line.isEmpty()) {
                int colon = line.indexOf(':');
                if (colon < 0) {
                    continue;
                }
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                if (key.equalsIgnoreCase("Filename")) {
                    filename = new File(value).getName(); // strip any path components
                } else if (key.equalsIgnoreCase("Length")) {
                    try {
                        expected = Long.parseLong(value);
                    } catch (NumberFormatException ignore) {
                        // leave expected as -1: read until the sender closes the socket
                    }
                }
            }

            File outFile = new File(outDir, filename);
            long total = 0;
            try (OutputStream fos = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[1024 * 1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    total += bytesRead;
                }
            }

            if (expected >= 0 && total != expected) {
                throw new IOException("Incomplete transfer: got " + total + " of " + expected + " bytes");
            }
            System.out.println("Saved '" + outFile.getName() + "' (" + total + " bytes) to " + outFile.getParent());
            return outFile;
        }
    }
}
