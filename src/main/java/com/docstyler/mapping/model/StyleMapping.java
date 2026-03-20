package com.docstyler.mapping.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "style_mappings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"profile_id", "unknown_style"})
})
public class StyleMapping {

    @Id
    @Column(length = 16)
    private String id;

    @Column(name = "profile_id", nullable = false, length = 16)
    private String profileId;

    @Column(name = "unknown_style", nullable = false)
    private String unknownStyle;

    @Column(name = "approved_style", nullable = false)
    private String approvedStyle;

    /** "manual" or "auto" */
    @Column(name = "mapping_source", length = 10)
    private String mappingSource = "manual";

    @Column(name = "times_used")
    private int timesUsed = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected StyleMapping() {}

    public StyleMapping(String profileId, String unknownStyle, String approvedStyle, String mappingSource) {
        this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        this.profileId = profileId;
        this.unknownStyle = unknownStyle;
        this.approvedStyle = approvedStyle;
        this.mappingSource = mappingSource;
    }

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public String getId() { return id; }
    public String getProfileId() { return profileId; }
    public String getUnknownStyle() { return unknownStyle; }
    public void setUnknownStyle(String unknownStyle) { this.unknownStyle = unknownStyle; }
    public String getApprovedStyle() { return approvedStyle; }
    public void setApprovedStyle(String approvedStyle) { this.approvedStyle = approvedStyle; }
    public String getMappingSource() { return mappingSource; }
    public void setMappingSource(String mappingSource) { this.mappingSource = mappingSource; }
    public int getTimesUsed() { return timesUsed; }
    public void incrementTimesUsed() { this.timesUsed++; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
