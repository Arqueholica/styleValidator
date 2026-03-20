package com.docstyler.document.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @Column(length = 16)
    private String id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    /** docx, idml */
    @Column(name = "file_type", nullable = false, length = 10)
    private String fileType;

    /** uploaded, extracted, compared, error */
    @Column(length = 20)
    private String status = "uploaded";

    @Column(name = "style_count")
    private int styleCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Document() {}

    public Document(String filename, String filePath, String fileType) {
        this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        this.filename = filename;
        this.filePath = filePath;
        this.fileType = fileType;
    }

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // Getters & setters
    public String getId() { return id; }
    public String getFilename() { return filename; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileType() { return fileType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getStyleCount() { return styleCount; }
    public void setStyleCount(int c) { this.styleCount = c; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
