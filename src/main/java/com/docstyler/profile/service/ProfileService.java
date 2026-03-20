package com.docstyler.profile.service;

import com.docstyler.profile.model.ApprovedStyle;
import com.docstyler.profile.model.StyleProfile;
import com.docstyler.profile.repository.StyleProfileRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final StyleProfileRepository repo;
    private final StyleProfileParser parser;
    private final ResourceLoader resourceLoader;

    @Value("${docstyler.profile-path:classpath:data/styles.xml}")
    private String defaultProfilePath;

    public ProfileService(StyleProfileRepository repo, StyleProfileParser parser, ResourceLoader resourceLoader) {
        this.repo = repo;
        this.parser = parser;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        // Load default profile on first run
        if (repo.findByIsDefaultTrue().isEmpty()) {
            try {
                Resource resource = resourceLoader.getResource(defaultProfilePath);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        List<ApprovedStyle> styles = parser.parse(xml);
                        StyleProfile profile = new StyleProfile("Default Profile", xml, styles.size());
                        profile.setDefault(true);
                        repo.save(profile);
                        log.info("Loaded default style profile with {} approved styles", styles.size());
                    }
                } else {
                    log.warn("Default style profile not found at: {}", defaultProfilePath);
                }
            } catch (IOException e) {
                log.error("Failed to load default style profile", e);
            }
        }
    }

    public StyleProfile getDefault() {
        return repo.findByIsDefaultTrue()
            .orElseThrow(() -> new EntityNotFoundException("No default style profile configured"));
    }

    public StyleProfile findOrThrow(String id) {
        return repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Style profile not found: " + id));
    }

    public List<StyleProfile> listAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public StyleProfile create(String name, String xml) {
        List<ApprovedStyle> styles = parser.parse(xml);
        return repo.save(new StyleProfile(name, xml, styles.size()));
    }

    public void delete(String id) {
        StyleProfile profile = findOrThrow(id);
        if (profile.isDefault()) throw new IllegalArgumentException("Cannot delete the default profile");
        repo.delete(profile);
    }

    public Set<String> getApprovedNames(String profileId) {
        StyleProfile profile = findOrThrow(profileId);
        return parser.extractApprovedNames(profile.getProfileXml());
    }

    public List<ApprovedStyle> getApprovedStyles(String profileId) {
        StyleProfile profile = findOrThrow(profileId);
        return parser.parse(profile.getProfileXml());
    }

    public Set<String> getDefaultApprovedNames() {
        StyleProfile profile = getDefault();
        return parser.extractApprovedNames(profile.getProfileXml());
    }
}
