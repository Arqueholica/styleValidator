package com.docstyler.comparison.controller;

import com.docstyler.comparison.model.ComparisonResult;
import com.docstyler.comparison.service.ComparisonService;
import com.docstyler.document.model.Document;
import com.docstyler.document.service.DocumentService;
import com.docstyler.extraction.model.DocumentStructure;
import com.docstyler.extraction.service.DocxExtractorService;
import com.docstyler.extraction.service.IdmlExtractorService;
import com.docstyler.profile.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/comparison")
public class ComparisonController {

    private final ComparisonService comparisonService;
    private final DocumentService documentService;
    private final DocxExtractorService docxExtractor;
    private final IdmlExtractorService idmlExtractor;
    private final ProfileService profileService;

    public ComparisonController(ComparisonService comparisonService,
                                 DocumentService documentService,
                                 DocxExtractorService docxExtractor,
                                 IdmlExtractorService idmlExtractor,
                                 ProfileService profileService) {
        this.comparisonService = comparisonService;
        this.documentService = documentService;
        this.docxExtractor = docxExtractor;
        this.idmlExtractor = idmlExtractor;
        this.profileService = profileService;
    }

    @PostMapping("/{documentId}")
    public ResponseEntity<ComparisonResult> compare(
            @PathVariable String documentId,
            @RequestParam(value = "profileId", required = false) String profileId) throws IOException {

        Document doc = documentService.findOrThrow(documentId);
        Path filePath = Path.of(doc.getFilePath());

        // Extract styles
        DocumentStructure structure;
        if ("idml".equals(doc.getFileType())) {
            structure = idmlExtractor.extract(filePath);
        } else {
            structure = docxExtractor.extract(filePath);
        }

        // Use default profile if none specified
        if (profileId == null || profileId.isBlank()) {
            profileId = profileService.getDefault().getId();
        }

        ComparisonResult result = comparisonService.compare(documentId, structure, profileId);

        // Update document status
        doc.setStatus("compared");
        doc.setStyleCount(structure.styles().size());
        documentService.save(doc);

        return ResponseEntity.ok(result);
    }
}
