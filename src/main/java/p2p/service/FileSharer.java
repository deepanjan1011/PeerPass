package p2p.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;

import p2p.utils.UploadUtils;

public class FileSharer {
    private HashMap<Integer, String> availableFiles;
    public FileSharer(){
        availableFiles = new HashMap<>();
    }
    public int offerFile(String filePath){
        int port;
        while(true){
            port = UploadUtils.generateCode();
            if(availableFiles.containsKey(port)){
                availableFiles.put(port, filePath);
                return port;
            }
        }
    }
    public void startFileServer(int port){
        String filePath = availableFiles.get(port);
        if(filePath == null){
            System.out.println("No File is associated with port: "+port);
            return;
        }
        try(ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Serving file "+ new File(filePath).getName() + "on port "+port);
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connection: " + clientSocket.getInetAddress());
            new Thread(new FileSenderHandler(clientSocket, filePath)).start();
        }catch(IOException ex){
            System.err.println("Error handling file server on port :"+port);
        }
    }
    private static class FileSenderHandler implements Runnable {
        private final Socket clientSocket;
        private final String filePath;

        public FileSenderHandler(Socket clientSocket, String filePath)
        {
            this.clientSocket = clientSocket;
            this.filePath = filePath;
        }


        @Override
        public void run() {
            try(FileInputStream fis = new FileInputStream(filePath)){
                OutputStream oos = clientSocket.getOutputStream();
                //sending filename as a header
                String fileName = new File(filePath).getName();
                String header = "Filename: "+fileName+"\n"; //---> e.g -> Filename:hola.txt
                oos.write(header.getBytes());
                //sending file content with larger buffer for better performance
                byte[] buffer = new byte[8192]; // 8KB buffer for better performance
                int bytesRead;
                long totalBytesSent = 0;
                while((bytesRead = fis.read(buffer)) != -1){
                    oos.write(buffer, 0, bytesRead);
                    totalBytesSent += bytesRead;
                    
                    // Clear buffer to prevent data contamination (Java equivalent of bytes.erase())
                    Arrays.fill(buffer, (byte) 0);
                }
                System.out.println("Total bytes sent: " + totalBytesSent);
                System.out.println("File "+fileName+" sent to "+clientSocket.getInetAddress());
            }catch(Exception ex){
                System.err.println("Error sending file to the client "+ex.getMessage());
            }finally {
                try{
                    clientSocket.close();
                }catch(Exception ex){
                    System.err.println("Error closing file socket "+ex.getMessage());
                }
            }
        }
    }
}
