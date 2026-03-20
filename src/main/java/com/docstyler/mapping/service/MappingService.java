package com.docstyler.mapping.service;

import com.docstyler.mapping.model.StyleMapping;
import com.docstyler.mapping.repository.StyleMappingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MappingService {

    private final StyleMappingRepository repo;

    public MappingService(StyleMappingRepository repo) {
        this.repo = repo;
    }

    /**
     * Creates or updates a mapping from an unknown style to an approved style.
     * This builds the persistent dictionary.
     */
    public StyleMapping createOrUpdate(String profileId, String unknownStyle, String approvedStyle, String source) {
        return repo.findByProfileIdAndUnknownStyleIgnoreCase(profileId, unknownStyle)
            .map(existing -> {
                existing.setApprovedStyle(approvedStyle);
                existing.setMappingSource(source);
                existing.incrementTimesUsed();
                return repo.save(existing);
            })
            .orElseGet(() -> repo.save(new StyleMapping(profileId, unknownStyle, approvedStyle, source)));
    }

    /**
     * Returns the mapping dictionary for a profile: unknownStyle(lowercase) -> approvedStyle
     */
    public Map<String, String> getMappingsForProfile(String profileId) {
        return repo.findByProfileId(profileId).stream()
            .collect(Collectors.toMap(
                m -> m.getUnknownStyle().toLowerCase(),
                StyleMapping::getApprovedStyle,
                (a, b) -> a // keep first if duplicate
            ));
    }

    public List<StyleMapping> listByProfile(String profileId) {
        return repo.findByProfileIdOrderByTimesUsedDesc(profileId);
    }

    public void delete(String id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("Mapping not found: " + id);
        repo.deleteById(id);
    }

    public StyleMapping findOrThrow(String id) {
        return repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Mapping not found: " + id));
    }
}
