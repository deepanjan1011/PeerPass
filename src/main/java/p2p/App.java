package p2p;

import java.util.Scanner;

import p2p.controller.FileController;
import p2p.service.FileReceiver;
import p2p.service.FileSharer;

public class App {
    private static volatile boolean isRunning = true;
    private static FileController fileController;

    public static void main(String[] args) {
        // Peer-to-peer CLI modes: file bytes travel directly between the two peers.
        if (args.length > 0) {
            runPeerMode(args);
            return;
        }
        try {
            fileController = new FileController(8080);
            fileController.start();
            System.out.println("PeerPass server started on port 8080");
            System.out.println("UI available at http://localhost:3000");
            
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    System.out.println("\nShutting down the server...");
                    shutdown();
                })
            );

            // Start input listener thread
            startInputListener();
            
            // Keep main thread alive
            while (isRunning) {
                Thread.sleep(100);
            }

        } catch (Exception ex) {
            System.err.println("Failed to start the server at port 8080: " + ex.getMessage());
        }
    }
    
    private static void runPeerMode(String[] args) {
        String mode = args[0];
        try {
            if (mode.equals("send") && args.length >= 2) {
                FileSharer sharer = new FileSharer();
                int port = sharer.offerFile(args[1]);
                sharer.startFileServer(port); // blocks, serving peers until stopped
            } else if (mode.equals("receive") && args.length >= 2) {
                String[] hostPort = args[1].split(":", 2);
                if (hostPort.length != 2) {
                    System.err.println("Usage: receive <host:port> [outDir]");
                    return;
                }
                String outDir = args.length >= 3 ? args[2] : ".";
                FileReceiver.receive(hostPort[0], Integer.parseInt(hostPort[1]), outDir);
            } else {
                System.err.println("Usage:\n  send <filePath>\n  receive <host:port> [outDir]");
            }
        } catch (Exception e) {
            System.err.println("Peer mode failed: " + e.getMessage());
        }
    }

    private static void startInputListener() {
        Thread inputThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Press Enter to stop the server...");
            
            while (isRunning) {
                try {
                    String input = scanner.nextLine();
                    if (input != null && input.trim().isEmpty()) {
                        System.out.println("Shutdown requested by user...");
                        shutdown();
                        break;
                    }
                } catch (Exception e) {
                    // Handle any input errors
                    break;
                }
            }
            scanner.close();
        });
        
        inputThread.setDaemon(true);
        inputThread.start();
    }
    
    private static void shutdown() {
        if (fileController != null) {
            fileController.stop();
        }
        isRunning = false;
        System.out.println("Server stopped successfully.");
        System.exit(0);
    }
}
