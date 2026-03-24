package com.docstyler.correction.controller;

import com.docstyler.correction.service.StyleCorrectionService;
import com.docstyler.document.model.Document;
import com.docstyler.document.service.DocumentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/correction")
public class CorrectionController {

    private final StyleCorrectionService correctionService;
    private final DocumentService documentService;

    public CorrectionController(StyleCorrectionService correctionService, DocumentService documentService) {
        this.correctionService = correctionService;
        this.documentService = documentService;
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadCorrectedDocument(
            @PathVariable String documentId,
            @RequestParam(value = "profileId", required = false) String profileId) throws IOException {

        Path correctedPath = correctionService.generateCorrectedDocument(documentId, profileId);
        String filename = correctionService.getCorrectedFilename(documentId);

        Document doc = documentService.findOrThrow(documentId);
        String contentType = "idml".equals(doc.getFileType())
            ? "application/octet-stream"
            : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        Resource resource = new FileSystemResource(correctedPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
