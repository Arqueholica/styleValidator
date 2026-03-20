package com.docstyler.extraction.model;

public record ExtractedStyle(
    String name,
    String type,       // "paragraph", "character", "table", "list"
    int usageCount,
    boolean isBuiltIn
) {}
