package com.docstyler.extraction.model;

import java.util.List;

public record DocumentStructure(
    List<ExtractedStyle> styles,
    List<StructureEntry> entries,
    int tableCount,
    int imageCount,
    int listCount,
    int pageCount
) {
    public record StructureEntry(
        int index,
        String type,       // "heading", "paragraph", "table", "image", "list"
        String styleName,
        String textPreview, // first 100 chars
        int level          // heading level (1-9), 0 for non-headings
    ) {}
}
