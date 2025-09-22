package p2p.model;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FileShare {
    private String fileId;
    private String filePath;
    private String filename;
    private long fileSize;
    private String passwordHash;
    private LocalDateTime createdAt;
    private AtomicInteger downloadCount;
    private int maxDownloads;
    
    public FileShare(String fileId, String filePath, String filename, long fileSize, String passwordHash) {
        this.fileId = fileId;
        this.filePath = filePath;
        this.filename = filename;
        this.fileSize = fileSize;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.downloadCount = new AtomicInteger(0);
        this.maxDownloads = -1; // Unlimited by default
    }
    
    // Getters and setters
    public String getFileId() { return fileId; }
    public String getFilePath() { return filePath; }
    public String getFilename() { return filename; }
    public long getFileSize() { return fileSize; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getDownloadCount() { return downloadCount.get(); }
    public int getMaxDownloads() { return maxDownloads; }
    
    public void setMaxDownloads(int maxDownloads) { this.maxDownloads = maxDownloads; }
    public boolean canDownload() { return maxDownloads == -1 || downloadCount.get() < maxDownloads; }
    public int incrementDownloadCount() { return downloadCount.incrementAndGet(); }
    
    // Password verification
    public boolean verifyPassword(String password) {
        if (passwordHash == null || passwordHash.isEmpty()) {
            return password == null || password.isEmpty();
        }
        return passwordHash.equals(hashPassword(password));
    }
    
    private String hashPassword(String password) {
        // Simple hash for now - in production, use proper hashing like BCrypt
        return String.valueOf(password.hashCode());
    }
}
