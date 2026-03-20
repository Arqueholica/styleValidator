package com.docstyler.mapping.controller;

import com.docstyler.mapping.model.StyleMapping;
import com.docstyler.mapping.service.MappingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mappings")
public class MappingController {

    private final MappingService mappingService;

    public MappingController(MappingService mappingService) {
        this.mappingService = mappingService;
    }

    @GetMapping
    public List<StyleMapping> listByProfile(@RequestParam String profileId) {
        return mappingService.listByProfile(profileId);
    }

    @PostMapping
    public ResponseEntity<StyleMapping> create(@RequestBody Map<String, String> body) {
        String profileId = body.get("profileId");
        String unknownStyle = body.get("unknownStyle");
        String approvedStyle = body.get("approvedStyle");
        String source = body.getOrDefault("source", "manual");

        if (profileId == null || unknownStyle == null || approvedStyle == null) {
            throw new IllegalArgumentException("profileId, unknownStyle, and approvedStyle are required");
        }

        StyleMapping mapping = mappingService.createOrUpdate(profileId, unknownStyle, approvedStyle, source);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapping);
    }

    /** Batch create mappings - accept close matches automatically */
    @PostMapping("/batch")
    public ResponseEntity<List<StyleMapping>> batchCreate(@RequestBody List<Map<String, String>> mappings) {
        List<StyleMapping> results = mappings.stream()
            .map(body -> mappingService.createOrUpdate(
                body.get("profileId"),
                body.get("unknownStyle"),
                body.get("approvedStyle"),
                body.getOrDefault("source", "auto")))
            .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        mappingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
