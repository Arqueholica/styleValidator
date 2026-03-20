package com.docstyler.document.service;

import com.docstyler.document.model.Document;
import com.docstyler.document.repository.DocumentRepository;
import com.docstyler.shared.util.FileUtils;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;

@Service
public class DocumentService {

    private static final List<String> ALLOWED = List.of(".docx", ".idml");

    private final DocumentRepository repo;

    @Value("${docstyler.upload-dir}")
    private String uploadDir;

    public DocumentService(DocumentRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Path.of(uploadDir));
    }

    public Document upload(MultipartFile file) throws IOException {
        String raw = file.getOriginalFilename();
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Filename required");

        String filename = FileUtils.sanitize(raw);
        String ext = FileUtils.getExtension(filename);

        if (!ALLOWED.contains(ext))
            throw new IllegalArgumentException("Unsupported file type: " + ext + ". Allowed: .docx, .idml");

        String fileType = ext.equals(".docx") ? "docx" : "idml";

        Document doc = new Document(filename, "", fileType);
        Path dir = Path.of(uploadDir, doc.getId());
        Files.createDirectories(dir);

        Path filePath = dir.resolve(filename).normalize().toAbsolutePath();
        if (!filePath.startsWith(dir.normalize().toAbsolutePath()))
            throw new IllegalArgumentException("Invalid filename");

        file.transferTo(filePath.toFile());
        doc.setFilePath(filePath.toString());
        return repo.save(doc);
    }

    public Document findOrThrow(String id) {
        return repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Document not found: " + id));
    }

    public List<Document> listAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public void delete(String id) {
        Document doc = findOrThrow(id);
        try {
            Path dir = Path.of(uploadDir, id);
            if (Files.exists(dir)) {
                try (var walk = Files.walk(dir)) {
                    walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(java.io.File::delete);
                }
            }
        } catch (IOException ignored) {}
        repo.delete(doc);
    }

    public Document save(Document doc) {
        return repo.save(doc);
    }
}
