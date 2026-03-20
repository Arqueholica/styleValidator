package com.docstyler.profile.repository;

import com.docstyler.profile.model.StyleProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface StyleProfileRepository extends JpaRepository<StyleProfile, String> {
    Optional<StyleProfile> findByIsDefaultTrue();
    List<StyleProfile> findAllByOrderByCreatedAtDesc();
}
