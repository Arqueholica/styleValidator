package com.docstyler.extraction.service;

import com.docstyler.extraction.model.DocumentStructure;
import com.docstyler.extraction.model.DocumentStructure.StructureEntry;
import com.docstyler.extraction.model.ExtractedStyle;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class IdmlExtractorService {

    public DocumentStructure extract(Path filePath) throws IOException {
        Map<String, ExtractedStyle> styleMap = new LinkedHashMap<>();
        List<StructureEntry> entries = new ArrayList<>();
        int tableCount = 0, imageCount = 0, listCount = 0, entryIndex = 0;

        try (ZipFile zip = new ZipFile(filePath.toFile())) {
            // 1. Parse Styles.xml for defined paragraph/character styles
            ZipEntry stylesEntry = zip.getEntry("Resources/Styles.xml");
            if (stylesEntry != null) {
                Document doc = parseXml(zip.getInputStream(stylesEntry));
                extractIdmlStyles(doc, "ParagraphStyle", "paragraph", styleMap);
                extractIdmlStyles(doc, "CharacterStyle", "character", styleMap);
                extractIdmlStyles(doc, "TableStyle", "table", styleMap);
                extractIdmlStyles(doc, "ObjectStyle", "object", styleMap);
            }

            // 2. Walk Story files for structure and usage counts
            Enumeration<? extends ZipEntry> zipEntries = zip.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry entry = zipEntries.nextElement();
                if (entry.getName().startsWith("Stories/Story_") && entry.getName().endsWith(".xml")) {
                    Document storyDoc = parseXml(zip.getInputStream(entry));
                    NodeList paragraphs = storyDoc.getElementsByTagName("ParagraphStyleRange");
                    for (int i = 0; i < paragraphs.getLength(); i++) {
                        Element psr = (Element) paragraphs.item(i);
                        String appliedStyle = psr.getAttribute("AppliedParagraphStyle");
                        String styleName = cleanIdmlStyleName(appliedStyle);

                        styleMap.merge(styleName,
                            new ExtractedStyle(styleName, "paragraph", 1, false),
                            (old, n) -> new ExtractedStyle(old.name(), old.type(), old.usageCount() + 1, old.isBuiltIn()));

                        // Extract text content
                        StringBuilder text = new StringBuilder();
                        NodeList contents = psr.getElementsByTagName("Content");
                        for (int j = 0; j < contents.getLength(); j++) {
                            text.append(contents.item(j).getTextContent());
                        }
                        String preview = text.length() > 100 ? text.substring(0, 100) + "..." : text.toString();

                        String type = "paragraph";
                        int level = 0;
                        // Check if it looks like a heading
                        String lower = styleName.toLowerCase();
                        if (lower.contains("head") || lower.contains("title") || lower.contains("subtitle")) {
                            type = "heading";
                            level = guessHeadingLevel(styleName);
                        }

                        entries.add(new StructureEntry(entryIndex++, type, styleName, preview, level));

                        // Count character styles used
                        NodeList charRanges = psr.getElementsByTagName("CharacterStyleRange");
                        for (int j = 0; j < charRanges.getLength(); j++) {
                            Element csr = (Element) charRanges.item(j);
                            String charStyle = cleanIdmlStyleName(csr.getAttribute("AppliedCharacterStyle"));
                            if (!charStyle.equals("[No character style]") && !charStyle.isBlank()) {
                                styleMap.merge(charStyle,
                                    new ExtractedStyle(charStyle, "character", 1, false),
                                    (old, n) -> new ExtractedStyle(old.name(), old.type(), old.usageCount() + 1, old.isBuiltIn()));
                            }
                        }
                    }

                    // Count tables
                    NodeList tables = storyDoc.getElementsByTagName("Table");
                    tableCount += tables.getLength();
                    for (int i = 0; i < tables.getLength(); i++) {
                        entries.add(new StructureEntry(entryIndex++, "table", "Table", "InDesign Table", 0));
                    }
                }

                // Count images from Graphic entries
                if (entry.getName().startsWith("Spreads/") && entry.getName().endsWith(".xml")) {
                    Document spreadDoc = parseXml(zip.getInputStream(entry));
                    NodeList images = spreadDoc.getElementsByTagName("Image");
                    imageCount += images.getLength();
                    NodeList epss = spreadDoc.getElementsByTagName("EPS");
                    imageCount += epss.getLength();
                    NodeList pdfs = spreadDoc.getElementsByTagName("PDF");
                    imageCount += pdfs.getLength();
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to parse IDML file: " + e.getMessage(), e);
        }

        return new DocumentStructure(new ArrayList<>(styleMap.values()), entries,
            tableCount, imageCount, listCount, 0);
    }

    private void extractIdmlStyles(Document doc, String tagName, String type, Map<String, ExtractedStyle> styleMap) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String name = cleanIdmlStyleName(el.getAttribute("Name"));
            if (!name.isBlank() && !name.startsWith("[")) {
                styleMap.putIfAbsent(name, new ExtractedStyle(name, type, 0, false));
            }
        }
    }

    /** Converts "ParagraphStyle/My Style Name" to "My Style Name" */
    private String cleanIdmlStyleName(String raw) {
        if (raw == null || raw.isBlank()) return "Unknown";
        // Remove prefix like "ParagraphStyle/" or "CharacterStyle/"
        int slash = raw.lastIndexOf('/');
        String name = slash >= 0 ? raw.substring(slash + 1) : raw;
        // Replace encoded spaces
        name = name.replace("%3a", ":").replace("%20", " ");
        return name;
    }

    private int guessHeadingLevel(String styleName) {
        String lower = styleName.toLowerCase();
        if (lower.contains("title") && !lower.contains("sub")) return 1;
        if (lower.contains("subtitle")) return 2;
        // Try to extract a number
        for (int i = 1; i <= 9; i++) {
            if (lower.contains(String.valueOf(i))) return i;
        }
        return 1; // default heading level
    }

    private Document parseXml(InputStream is) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(is);
    }
}
