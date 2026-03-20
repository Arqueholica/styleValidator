package com.docstyler.profile.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "style_profiles")
public class StyleProfile {

    @Id
    @Column(length = 16)
    private String id;

    @Column(name = "profile_name", nullable = false)
    private String profileName;

    @Lob
    @Column(name = "profile_xml", nullable = false)
    private String profileXml;

    @Column(name = "style_count")
    private int styleCount;

    @Column(name = "is_default")
    private boolean isDefault;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected StyleProfile() {}

    public StyleProfile(String profileName, String profileXml, int styleCount) {
        this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        this.profileName = profileName;
        this.profileXml = profileXml;
        this.styleCount = styleCount;
    }

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    public String getId() { return id; }
    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }
    public String getProfileXml() { return profileXml; }
    public void setProfileXml(String profileXml) { this.profileXml = profileXml; }
    public int getStyleCount() { return styleCount; }
    public void setStyleCount(int styleCount) { this.styleCount = styleCount; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
