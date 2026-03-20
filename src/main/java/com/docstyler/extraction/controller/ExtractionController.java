package com.docstyler.extraction.controller;

import com.docstyler.document.model.Document;
import com.docstyler.document.service.DocumentService;
import com.docstyler.extraction.model.DocumentStructure;
import com.docstyler.extraction.service.DocxExtractorService;
import com.docstyler.extraction.service.IdmlExtractorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/extraction")
public class ExtractionController {

    private final DocumentService documentService;
    private final DocxExtractorService docxExtractor;
    private final IdmlExtractorService idmlExtractor;

    public ExtractionController(DocumentService documentService,
                                 DocxExtractorService docxExtractor,
                                 IdmlExtractorService idmlExtractor) {
        this.documentService = documentService;
        this.docxExtractor = docxExtractor;
        this.idmlExtractor = idmlExtractor;
    }

    @PostMapping("/{documentId}")
    public ResponseEntity<DocumentStructure> extract(@PathVariable String documentId) throws IOException {
        Document doc = documentService.findOrThrow(documentId);
        Path filePath = Path.of(doc.getFilePath());

        DocumentStructure structure;
        if ("idml".equals(doc.getFileType())) {
            structure = idmlExtractor.extract(filePath);
        } else {
            structure = docxExtractor.extract(filePath);
        }

        // Update document status
        doc.setStatus("extracted");
        doc.setStyleCount(structure.styles().size());
        documentService.save(doc);

        return ResponseEntity.ok(structure);
    }
}
