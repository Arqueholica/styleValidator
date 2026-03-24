package com.docstyler.correction.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
public class IdmlCorrectionService {

    /**
     * Creates a corrected copy of an IDML file with style names renamed
     * according to the provided mapping.
     *
     * @param sourcePath path to the original IDML file
     * @param outputPath path for the corrected IDML file
     * @param styleMap   map of documentStyleName -> approvedStyleName
     */
    public void correctStyles(Path sourcePath, Path outputPath, Map<String, String> styleMap) throws IOException {
        try (ZipFile sourceZip = new ZipFile(sourcePath.toFile());
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputPath.toFile()))) {

            Enumeration<? extends ZipEntry> entries = sourceZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                zos.putNextEntry(new ZipEntry(entry.getName()));

                if (shouldProcessEntry(entry.getName())) {
                    // Process XML entries that contain style references
                    byte[] corrected = processXmlEntry(sourceZip.getInputStream(entry), styleMap, entry.getName());
                    zos.write(corrected);
                } else {
                    // Copy non-XML entries as-is
                    try (InputStream is = sourceZip.getInputStream(entry)) {
                        is.transferTo(zos);
                    }
                }
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to correct IDML file: " + e.getMessage(), e);
        }
    }

    private boolean shouldProcessEntry(String entryName) {
        return entryName.equals("Resources/Styles.xml")
            || (entryName.startsWith("Stories/Story_") && entryName.endsWith(".xml"));
    }

    private byte[] processXmlEntry(InputStream is, Map<String, String> styleMap, String entryName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(is);

        if (entryName.equals("Resources/Styles.xml")) {
            correctStyleDefinitions(doc, styleMap);
        }

        if (entryName.startsWith("Stories/Story_")) {
            correctStoryReferences(doc, styleMap);
        }

        return documentToBytes(doc);
    }

    private void correctStyleDefinitions(Document doc, Map<String, String> styleMap) {
        // Process ParagraphStyle, CharacterStyle, TableStyle, ObjectStyle elements
        String[] styleTypes = {"ParagraphStyle", "CharacterStyle", "TableStyle", "ObjectStyle"};
        for (String styleType : styleTypes) {
            NodeList nodes = doc.getElementsByTagName(styleType);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String nameAttr = el.getAttribute("Name");
                if (nameAttr != null && !nameAttr.isBlank()) {
                    String cleanName = cleanIdmlStyleName(nameAttr);
                    if (styleMap.containsKey(cleanName)) {
                        String newName = styleMap.get(cleanName);
                        // Reconstruct the full IDML-style name with prefix
                        String prefix = getStylePrefix(nameAttr);
                        String encodedNewName = encodeIdmlStyleName(newName);
                        el.setAttribute("Name", prefix + encodedNewName);

                        // Also update Self attribute if present
                        String self = el.getAttribute("Self");
                        if (self != null && !self.isBlank()) {
                            String selfPrefix = getStylePrefix(self);
                            el.setAttribute("Self", selfPrefix + encodedNewName);
                        }
                    }
                }
            }
        }
    }

    private void correctStoryReferences(Document doc, Map<String, String> styleMap) {
        // Update AppliedParagraphStyle attributes
        correctAttributeReferences(doc, "ParagraphStyleRange", "AppliedParagraphStyle", "ParagraphStyle/", styleMap);
        // Update AppliedCharacterStyle attributes
        correctAttributeReferences(doc, "CharacterStyleRange", "AppliedCharacterStyle", "CharacterStyle/", styleMap);
    }

    private void correctAttributeReferences(Document doc, String tagName, String attrName,
                                             String defaultPrefix, Map<String, String> styleMap) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String attrValue = el.getAttribute(attrName);
            if (attrValue != null && !attrValue.isBlank()) {
                String cleanName = cleanIdmlStyleName(attrValue);
                if (styleMap.containsKey(cleanName)) {
                    String newName = styleMap.get(cleanName);
                    String prefix = getStylePrefix(attrValue);
                    if (prefix.isEmpty()) prefix = defaultPrefix;
                    el.setAttribute(attrName, prefix + encodeIdmlStyleName(newName));
                }
            }
        }
    }

    /** Extracts prefix like "ParagraphStyle/" from "ParagraphStyle/MyStyle" */
    private String getStylePrefix(String raw) {
        int slash = raw.lastIndexOf('/');
        return slash >= 0 ? raw.substring(0, slash + 1) : "";
    }

    /** Converts "ParagraphStyle/My Style Name" to "My Style Name" */
    private String cleanIdmlStyleName(String raw) {
        if (raw == null || raw.isBlank()) return "";
        int slash = raw.lastIndexOf('/');
        String name = slash >= 0 ? raw.substring(slash + 1) : raw;
        name = name.replace("%3a", ":").replace("%20", " ");
        return name;
    }

    /** Encodes a style name for IDML (spaces become %20, colons become %3a) */
    private String encodeIdmlStyleName(String name) {
        return name.replace(":", "%3a").replace(" ", "%20");
    }

    private byte[] documentToBytes(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }
}
