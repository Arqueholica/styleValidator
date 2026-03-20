package com.docstyler.extraction.service;

import com.docstyler.extraction.model.DocumentStructure;
import com.docstyler.extraction.model.DocumentStructure.StructureEntry;
import com.docstyler.extraction.model.ExtractedStyle;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocxExtractorService {

    private static final Pattern HEADING_PATTERN = Pattern.compile("(?i)heading\\s*(\\d+)");

    private static final Set<String> BUILT_IN = Set.of(
        "Normal", "Default Paragraph Font", "No List",
        "Heading 1", "Heading 2", "Heading 3", "Heading 4", "Heading 5", "Heading 6",
        "Heading 7", "Heading 8", "Heading 9", "Title", "Subtitle",
        "Strong", "Emphasis", "No Spacing", "List Paragraph",
        "Quote", "Intense Quote", "Subtle Emphasis", "Intense Emphasis",
        "Subtle Reference", "Intense Reference", "Book Title",
        "TOC Heading", "Table of Contents", "Block Text"
    );

    public DocumentStructure extract(Path filePath) throws IOException {
        try (var fis = new FileInputStream(filePath.toFile());
             var doc = new XWPFDocument(fis)) {

            Map<String, ExtractedStyle> styleMap = new LinkedHashMap<>();
            List<StructureEntry> entries = new ArrayList<>();
            int tableCount = 0, imageCount = 0, listCount = 0, entryIndex = 0;

            // Walk body elements for structure and used style counts
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph para) {
                    String styleId = para.getStyle();
                    String resolvedName = resolveStyleName(doc, styleId);

                    styleMap.merge(resolvedName,
                        new ExtractedStyle(resolvedName, "paragraph", 1, BUILT_IN.contains(resolvedName)),
                        (old, n) -> new ExtractedStyle(old.name(), old.type(), old.usageCount() + 1, old.isBuiltIn()));

                    int level = getHeadingLevel(resolvedName);
                    String type = level > 0 ? "heading" : "paragraph";

                    if (para.getNumID() != null) { type = "list"; listCount++; }

                    long picCount = para.getRuns().stream()
                        .mapToLong(r -> r.getEmbeddedPictures().size()).sum();
                    if (picCount > 0) imageCount += (int) picCount;

                    String text = para.getText();
                    String preview = text == null ? "" : (text.length() > 100 ? text.substring(0, 100) + "..." : text);

                    entries.add(new StructureEntry(entryIndex++, type, resolvedName, preview, level));

                } else if (element instanceof XWPFTable table) {
                    tableCount++;
                    entries.add(new StructureEntry(entryIndex++, "table", "Table",
                        "Table with " + table.getNumberOfRows() + " rows, " +
                        (table.getRow(0) != null ? table.getRow(0).getTableCells().size() : 0) + " columns", 0));
                }
            }

            return new DocumentStructure(new ArrayList<>(styleMap.values()), entries,
                tableCount, imageCount, listCount, 0);
        }
    }

    private String resolveStyleName(XWPFDocument doc, String styleId) {
        if (styleId == null || styleId.isBlank()) return "Normal";
        try {
            XWPFStyles styles = doc.getStyles();
            if (styles != null) {
                XWPFStyle style = styles.getStyle(styleId);
                if (style != null && style.getName() != null) return style.getName();
            }
        } catch (Exception ignored) {}
        return styleId;
    }

    private int getHeadingLevel(String styleName) {
        if (styleName == null) return 0;
        if (styleName.equalsIgnoreCase("Title")) return 1;
        if (styleName.equalsIgnoreCase("Subtitle")) return 2;
        Matcher m = HEADING_PATTERN.matcher(styleName);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }
}
