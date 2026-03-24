package com.docstyler.correction.service;

import com.docstyler.comparison.model.ComparisonResult;
import com.docstyler.comparison.model.ComparisonResult.MatchType;
import com.docstyler.comparison.model.ComparisonResult.StyleMatch;
import com.docstyler.comparison.service.ComparisonService;
import com.docstyler.document.model.Document;
import com.docstyler.document.service.DocumentService;
import com.docstyler.extraction.model.DocumentStructure;
import com.docstyler.extraction.service.DocxExtractorService;
import com.docstyler.extraction.service.IdmlExtractorService;
import com.docstyler.profile.service.ProfileService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StyleCorrectionService {

    private final DocumentService documentService;
    private final ComparisonService comparisonService;
    private final DocxExtractorService docxExtractor;
    private final IdmlExtractorService idmlExtractor;
    private final DocxCorrectionService docxCorrection;
    private final IdmlCorrectionService idmlCorrection;
    private final ProfileService profileService;

    public StyleCorrectionService(DocumentService documentService,
                                   ComparisonService comparisonService,
                                   DocxExtractorService docxExtractor,
                                   IdmlExtractorService idmlExtractor,
                                   DocxCorrectionService docxCorrection,
                                   IdmlCorrectionService idmlCorrection,
                                   ProfileService profileService) {
        this.documentService = documentService;
        this.comparisonService = comparisonService;
        this.docxExtractor = docxExtractor;
        this.idmlExtractor = idmlExtractor;
        this.docxCorrection = docxCorrection;
        this.idmlCorrection = idmlCorrection;
        this.profileService = profileService;
    }

    /**
     * Generates a corrected document with styles renamed to their approved equivalents.
     * Returns the path to the corrected file.
     */
    public Path generateCorrectedDocument(String documentId, String profileId) throws IOException {
        Document doc = documentService.findOrThrow(documentId);
        Path sourcePath = Path.of(doc.getFilePath());

        if (profileId == null || profileId.isBlank()) {
            profileId = profileService.getDefault().getId();
        }

        // Run comparison to get the style mapping
        DocumentStructure structure;
        if ("idml".equals(doc.getFileType())) {
            structure = idmlExtractor.extract(sourcePath);
        } else {
            structure = docxExtractor.extract(sourcePath);
        }

        ComparisonResult result = comparisonService.compare(documentId, structure, profileId);

        // Build the rename map: documentStyle -> approvedStyle
        // Only include styles that actually need renaming (matched to a different name)
        Map<String, String> styleMap = buildStyleMap(result);

        if (styleMap.isEmpty()) {
            // No corrections needed - return a copy of the original
            Path outputPath = buildOutputPath(doc);
            Files.copy(sourcePath, outputPath);
            return outputPath;
        }

        // Generate corrected file
        Path outputPath = buildOutputPath(doc);
        Files.createDirectories(outputPath.getParent());

        if ("idml".equals(doc.getFileType())) {
            idmlCorrection.correctStyles(sourcePath, outputPath, styleMap);
        } else {
            docxCorrection.correctStyles(sourcePath, outputPath, styleMap);
        }

        return outputPath;
    }

    /**
     * Builds a map of documentStyleName -> approvedStyleName from comparison results.
     * Only includes styles that need renaming (where the names differ).
     */
    private Map<String, String> buildStyleMap(ComparisonResult result) {
        Map<String, String> map = new LinkedHashMap<>();
        for (StyleMatch match : result.matches()) {
            if (match.matchedApprovedStyle() != null
                && (match.matchType() == MatchType.EXACT || match.matchType() == MatchType.CLOSE_MATCH)
                && !match.documentStyle().equals(match.matchedApprovedStyle())) {
                map.put(match.documentStyle(), match.matchedApprovedStyle());
            }
        }
        return map;
    }

    /**
     * Builds the output file path with "_preprocess" appended before the extension.
     * E.g., "document.docx" -> "document_preprocess.docx"
     */
    private Path buildOutputPath(Document doc) {
        String filename = doc.getFilename();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex >= 0 ? filename.substring(0, dotIndex) : filename;
        String extension = dotIndex >= 0 ? filename.substring(dotIndex) : "";
        String outputFilename = baseName + "_preprocess" + extension;

        // Place in the same upload directory as the original
        return Path.of(doc.getFilePath()).getParent().resolve(outputFilename);
    }

    /**
     * Returns the download filename for the corrected document.
     */
    public String getCorrectedFilename(String documentId) {
        Document doc = documentService.findOrThrow(documentId);
        String filename = doc.getFilename();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex >= 0 ? filename.substring(0, dotIndex) : filename;
        String extension = dotIndex >= 0 ? filename.substring(dotIndex) : "";
        return baseName + "_preprocess" + extension;
    }
}
