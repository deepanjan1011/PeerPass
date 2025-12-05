package p2p.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import p2p.service.FileSharer;

public class FileController {
    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;

    public FileController(int port) throws IOException {
        this.fileSharer = new FileSharer();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "peerlink-uploads";
        this.executorService = Executors.newFixedThreadPool(10);
        
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }
        
        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/", new CORSHandler());
        
        server.setExecutor(executorService);
    }
    
    public void start() {
        server.start();
        System.out.println("API server started on port " + server.getAddress().getPort());
    }
    
    public void stop() {
        server.stop(0);
        executorService.shutdown();
        System.out.println("API server stopped");
    }
    
    private class CORSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            String response = "Not Found";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    private static class MultipartParser {
        private final byte[] data;
        private final String boundary;
        
        public MultipartParser(byte[] data, String boundary) {
            this.data = data;
            this.boundary = boundary;
        }
        
        public ParseResult parse() {
            try {
                String dataAsString = new String(data);
                
                String filenameMarker = "filename=\"";
                int filenameStart = dataAsString.indexOf(filenameMarker);
                if (filenameStart == -1) {
                    return null;
                }
                
                filenameStart += filenameMarker.length();
                int filenameEnd = dataAsString.indexOf("\"", filenameStart);
                String filename = dataAsString.substring(filenameStart, filenameEnd);
                
                String contentTypeMarker = "Content-Type: ";
                int contentTypeStart = dataAsString.indexOf(contentTypeMarker, filenameEnd);
                String contentType = "application/octet-stream"; // Default
                
                if (contentTypeStart != -1) {
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd = dataAsString.indexOf("\r\n", contentTypeStart);
                    contentType = dataAsString.substring(contentTypeStart, contentTypeEnd);
                }
                
                String headerEndMarker = "\r\n\r\n";
                int headerEnd = dataAsString.indexOf(headerEndMarker);
                if (headerEnd == -1) {
                    return null;
                }
                
                int contentStart = headerEnd + headerEndMarker.length();
                
                byte[] boundaryBytes = ("\r\n--" + boundary + "--").getBytes();
                int contentEnd = findSequence(data, boundaryBytes, contentStart);
                
                if (contentEnd == -1) {
                    boundaryBytes = ("\r\n--" + boundary).getBytes();
                    contentEnd = findSequence(data, boundaryBytes, contentStart);
                }
                
                if (contentEnd == -1 || contentEnd <= contentStart) {
                    return null;
                }
                
                byte[] fileContent = new byte[contentEnd - contentStart];
                System.arraycopy(data, contentStart, fileContent, 0, fileContent.length);
                
                return new ParseResult(filename, contentType, fileContent);
            } catch (Exception e) {
                System.err.println("Error parsing multipart data: " + e.getMessage());
                return null;
            }
        }
        
        private int findSequence(byte[] data, byte[] sequence, int startPos) {
            outer:
            for (int i = startPos; i <= data.length - sequence.length; i++) {
                for (int j = 0; j < sequence.length; j++) {
                    if (data[i + j] != sequence[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }
        
        public static class ParseResult {
            public final String filename;
            public final String contentType;
            public final byte[] fileContent;
            
            public ParseResult(String filename, String contentType, byte[] fileContent) {
                this.filename = filename;
                this.contentType = contentType;
                this.fileContent = fileContent;
            }
        }
    }
    
    private static class HttpExchangeRequestContext implements RequestContext {
        private final HttpExchange exchange;

        public HttpExchangeRequestContext(HttpExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public String getCharacterEncoding() {
            return null;
        }

        @Override
        public String getContentType() {
            return exchange.getRequestHeaders().getFirst("Content-Type");
        }

        @Override
        public int getContentLength() {
            try {
                String len = exchange.getRequestHeaders().getFirst("Content-Length");
                return len == null ? -1 : Integer.parseInt(len);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return exchange.getRequestBody();
        }
    }

    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    
            // Handle CORS preflight
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
    
            // Only allow POST after preflight
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
    
            Headers requestHeaders = exchange.getRequestHeaders();
            String contentType = requestHeaders.getFirst("Content-Type");
    
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                String response = "Bad Request: Content-Type must be multipart/form-data";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
    
            try {
                DiskFileItemFactory factory = new DiskFileItemFactory();
                factory.setSizeThreshold(0);
                factory.setRepository(new File(uploadDir));
    
                FileUpload upload = new FileUpload(factory);
                upload.setFileSizeMax(-1);
                upload.setSizeMax(-1);
    
                HttpExchangeRequestContext ctx = new HttpExchangeRequestContext(exchange);
                FileItemIterator iter = upload.getItemIterator(ctx);
    
                String savedFilePath = null;
                String filename = null;
    
                while (iter.hasNext()) {
                    FileItemStream item = iter.next();
                    if (item.isFormField()) continue;
    
                    filename = item.getName();
                    if (filename == null || filename.trim().isEmpty()) {
                        filename = "unnamed-file";
                    }
                    String uniqueFilename = UUID.randomUUID().toString() + "_" + new File(filename).getName();
                    String filePath = uploadDir + File.separator + uniqueFilename;
    
                    try (InputStream in = item.openStream();
                         OutputStream out = new FileOutputStream(filePath)) {
    
                        byte[] buffer = new byte[1024 * 1024];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                    savedFilePath = filePath;
                    break;
                }
    
                if (savedFilePath == null) {
                    String response = "Bad Request: No file part found";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }
    
                int port = fileSharer.offerFile(savedFilePath);
    
                String jsonResponse = "{\"port\": " + port + "}";
                headers.add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
    
            } catch (Exception e) {
                System.err.println("Error processing file upload: " + e.getMessage());
                String response = "Server error: " + e.getMessage();
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
    
    
    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            // Handle preflight
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            String portStr = path.substring(path.lastIndexOf('/') + 1);
            
            // No password needed - simple download
            
            try {
                int port = Integer.parseInt(portStr);

                // Direct-from-disk streaming for multi-receiver reliability
                String directFilePath = fileSharer.getFilePath(port);
                if (directFilePath != null) {
                    File file = new File(directFilePath);
                    if (!file.exists() || !file.isFile()) {
                        String response = "Not Found: Shared file is missing";
                        exchange.sendResponseHeaders(404, response.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); }
                        return;
                    }

                    String filename = file.getName();
                    long contentLength = file.length();

                    headers.add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                    String mime = null;
                    try { mime = java.net.URLConnection.guessContentTypeFromName(filename); } catch (Exception ignore) {}
                    if (mime == null || mime.trim().isEmpty()) mime = "application/octet-stream";
                    headers.add("Content-Type", mime);
                    headers.add("Access-Control-Expose-Headers", "Content-Disposition");
                    headers.add("X-Content-Type-Options", "nosniff");
                    headers.add("X-Download-Options", "noopen");
                    headers.add("Content-Security-Policy", "default-src 'none'");
                    headers.add("Cache-Control", "private, no-cache, no-store, must-revalidate");
                    headers.add("Pragma", "no-cache");
                    headers.add("Expires", "0");

                    if (contentLength >= 0) {
                        exchange.sendResponseHeaders(200, contentLength);
                    } else {
                        exchange.sendResponseHeaders(200, -1);
                    }
                    try (FileInputStream fis = new FileInputStream(file);
                         OutputStream os = exchange.getResponseBody()) {
                        byte[] buffer = new byte[1024 * 1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                    return;
                }

                // Code not registered
                String response = "Not Found: Invalid or expired code";
                exchange.sendResponseHeaders(404, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); }

            } catch (NumberFormatException e) {
                String response = "Bad Request: Invalid port number";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}