package p2p;

import java.util.Scanner;

import p2p.controller.FileController;

public class App {
    private static volatile boolean isRunning = true;
    private static FileController fileController;
    
    public static void main(String[] args) {
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
