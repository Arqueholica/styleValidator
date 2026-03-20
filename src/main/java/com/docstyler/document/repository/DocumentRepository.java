package com.docstyler.document.repository;

import com.docstyler.document.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, String> {
    List<Document> findAllByOrderByCreatedAtDesc();
}
