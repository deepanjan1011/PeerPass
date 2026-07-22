package p2p;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import p2p.service.FileReceiver;
import p2p.service.FileSharer;

/**
 * Proves the P2P path end to end: a sender serves a file on a dynamic port and a
 * receiver connects directly over a socket and pulls it down, byte-for-byte.
 */
public class P2PTransferTest {

    @Test
    public void receiverGetsExactBytesFromSender(@TempDir File tmp) throws Exception {
        // A payload larger than the 1 MB chunk buffer to exercise the streaming loop.
        byte[] payload = new byte[3 * 1024 * 1024 + 777];
        new Random(42).nextBytes(payload);

        File source = new File(tmp, "source.bin");
        Files.write(source.toPath(), payload);

        FileSharer sharer = new FileSharer();
        int port = sharer.offerFile(source.getAbsolutePath());

        Thread server = new Thread(() -> sharer.startFileServer(port));
        server.setDaemon(true);
        server.start();

        File outDir = new File(tmp, "out");
        outDir.mkdirs();

        File received = null;
        // The server binds asynchronously; retry the connect briefly.
        for (int attempt = 0; attempt < 50 && received == null; attempt++) {
            try {
                received = FileReceiver.receive("127.0.0.1", port, outDir.getAbsolutePath());
            } catch (Exception e) {
                Thread.sleep(100);
            }
        }

        assertEquals("source.bin", received.getName());
        assertArrayEquals(payload, Files.readAllBytes(received.toPath()));
    }
}
