package com.docstyler.mapping.repository;

import com.docstyler.mapping.model.StyleMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StyleMappingRepository extends JpaRepository<StyleMapping, String> {
    List<StyleMapping> findByProfileId(String profileId);
    Optional<StyleMapping> findByProfileIdAndUnknownStyleIgnoreCase(String profileId, String unknownStyle);
    List<StyleMapping> findByProfileIdOrderByTimesUsedDesc(String profileId);
}
