package p2p.service;

import p2p.model.FileShare;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class FileShareManager {
    private static final FileShareManager instance = new FileShareManager();
    private final ConcurrentHashMap<String, FileShare> fileShares = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> portToFileId = new ConcurrentHashMap<>();
    
    private FileShareManager() {}
    
    public static FileShareManager getInstance() {
        return instance;
    }
    
    public String createFileShare(String filePath, String filename, long fileSize, String password) {
        String fileId = UUID.randomUUID().toString();
        String passwordHash = password != null && !password.isEmpty() ? hashPassword(password) : null;
        
        FileShare fileShare = new FileShare(fileId, filePath, filename, fileSize, passwordHash);
        fileShares.put(fileId, fileShare);
        
        return fileId;
    }
    
    public int offerFile(String fileId) {
        FileShare fileShare = fileShares.get(fileId);
        if (fileShare == null) {
            throw new IllegalArgumentException("File share not found: " + fileId);
        }
        
        // Find an available port
        int port;
        do {
            port = p2p.utils.UploadUtils.generateCode();
        } while (portToFileId.containsKey(port));
        
        portToFileId.put(port, fileId);
        return port;
    }
    
    public FileShare getFileShare(String fileId) {
        return fileShares.get(fileId);
    }
    
    public FileShare getFileShareByPort(int port) {
        String fileId = portToFileId.get(port);
        return fileId != null ? fileShares.get(fileId) : null;
    }
    
    public boolean verifyPassword(String fileId, String password) {
        FileShare fileShare = fileShares.get(fileId);
        if (fileShare == null) return false;
        return fileShare.verifyPassword(password);
    }
    
    public boolean canDownload(String fileId) {
        FileShare fileShare = fileShares.get(fileId);
        if (fileShare == null) return false;
        return fileShare.canDownload();
    }
    
    public void recordDownload(String fileId) {
        FileShare fileShare = fileShares.get(fileId);
        if (fileShare != null) {
            fileShare.incrementDownloadCount();
        }
    }
    
    public void cleanupExpiredShares() {
        LocalDateTime now = LocalDateTime.now();
        fileShares.entrySet().removeIf(entry -> {
            FileShare share = entry.getValue();
            // Remove shares older than 24 hours
            return ChronoUnit.HOURS.between(share.getCreatedAt(), now) > 24;
        });
    }
    
    private String hashPassword(String password) {
        // Simple hash for now - in production, use proper hashing like BCrypt
        return String.valueOf(password.hashCode());
    }
    
    public Map<String, Object> getShareInfo(String fileId) {
        FileShare fileShare = fileShares.get(fileId);
        if (fileShare == null) return null;
        
        return Map.of(
            "fileId", fileId,
            "filename", fileShare.getFilename(),
            "fileSize", fileShare.getFileSize(),
            "downloadCount", fileShare.getDownloadCount(),
            "maxDownloads", fileShare.getMaxDownloads(),
            "hasPassword", fileShare.getPasswordHash() != null,
            "createdAt", fileShare.getCreatedAt().toString()
        );
    }
}
