package com.docstyler.reporting.controller;

import com.docstyler.comparison.model.ComparisonResult;
import com.docstyler.comparison.service.ComparisonService;
import com.docstyler.document.model.Document;
import com.docstyler.document.service.DocumentService;
import com.docstyler.extraction.model.DocumentStructure;
import com.docstyler.extraction.service.DocxExtractorService;
import com.docstyler.extraction.service.IdmlExtractorService;
import com.docstyler.profile.service.ProfileService;
import com.docstyler.reporting.service.ReportService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ComparisonService comparisonService;
    private final DocumentService documentService;
    private final DocxExtractorService docxExtractor;
    private final IdmlExtractorService idmlExtractor;
    private final ProfileService profileService;

    public ReportController(ReportService reportService, ComparisonService comparisonService,
                             DocumentService documentService, DocxExtractorService docxExtractor,
                             IdmlExtractorService idmlExtractor, ProfileService profileService) {
        this.reportService = reportService;
        this.comparisonService = comparisonService;
        this.documentService = documentService;
        this.docxExtractor = docxExtractor;
        this.idmlExtractor = idmlExtractor;
        this.profileService = profileService;
    }

    @GetMapping("/{documentId}/html")
    public ResponseEntity<byte[]> downloadHtml(
            @PathVariable String documentId,
            @RequestParam(value = "profileId", required = false) String profileId) throws IOException {

        ComparisonResult result = runComparison(documentId, profileId);
        Document doc = documentService.findOrThrow(documentId);
        String html = reportService.generateHtmlReport(result, doc.getFilename());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("style-report-" + doc.getFilename().replaceAll("\\.[^.]+$", "") + ".html")
            .build());

        return new ResponseEntity<>(html.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    @GetMapping("/{documentId}/csv")
    public ResponseEntity<byte[]> downloadCsv(
            @PathVariable String documentId,
            @RequestParam(value = "profileId", required = false) String profileId) throws IOException {

        ComparisonResult result = runComparison(documentId, profileId);
        Document doc = documentService.findOrThrow(documentId);
        String csv = reportService.generateCsvReport(result);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("style-report-" + doc.getFilename().replaceAll("\\.[^.]+$", "") + ".csv")
            .build());

        return new ResponseEntity<>(csv.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    private ComparisonResult runComparison(String documentId, String profileId) throws IOException {
        Document doc = documentService.findOrThrow(documentId);
        Path filePath = Path.of(doc.getFilePath());

        DocumentStructure structure;
        if ("idml".equals(doc.getFileType())) {
            structure = idmlExtractor.extract(filePath);
        } else {
            structure = docxExtractor.extract(filePath);
        }

        if (profileId == null || profileId.isBlank()) {
            profileId = profileService.getDefault().getId();
        }

        return comparisonService.compare(documentId, structure, profileId);
    }
}
