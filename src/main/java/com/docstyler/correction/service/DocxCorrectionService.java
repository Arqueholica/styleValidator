package com.docstyler.correction.service;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class DocxCorrectionService {

    private static final String WORD_ML_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    /**
     * Creates a corrected copy of a DOCX file with style names renamed
     * according to the provided mapping.
     */
    public void correctStyles(Path sourcePath, Path outputPath, Map<String, String> styleMap) throws IOException {
        // Copy the file first, then modify the copy
        Files.copy(sourcePath, outputPath);

        try (OPCPackage pkg = OPCPackage.open(outputPath.toFile())) {
            // Find and modify the styles part (word/styles.xml)
            for (PackagePart part : pkg.getParts()) {
                String partName = part.getPartName().getName();
                if (partName.equals("/word/styles.xml")) {
                    modifyStylesPart(part, styleMap);
                }
            }
            pkg.flush();
        } catch (IOException e) {
            Files.deleteIfExists(outputPath);
            throw e;
        } catch (Exception e) {
            Files.deleteIfExists(outputPath);
            throw new IOException("Failed to correct DOCX styles: " + e.getMessage(), e);
        }
    }

    private void modifyStylesPart(PackagePart part, Map<String, String> styleMap) throws Exception {
        Document doc;
        try (InputStream is = part.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(is);
        }

        // Find all w:style elements and rename w:name values
        NodeList styleNodes = doc.getElementsByTagNameNS(WORD_ML_NS, "style");
        for (int i = 0; i < styleNodes.getLength(); i++) {
            Element styleEl = (Element) styleNodes.item(i);

            // Get the w:name child element
            NodeList nameNodes = styleEl.getElementsByTagNameNS(WORD_ML_NS, "name");
            if (nameNodes.getLength() > 0) {
                Element nameEl = (Element) nameNodes.item(0);
                String currentName = nameEl.getAttributeNS(WORD_ML_NS, "val");
                if (currentName.isEmpty()) {
                    currentName = nameEl.getAttribute("w:val");
                }

                if (styleMap.containsKey(currentName)) {
                    String newName = styleMap.get(currentName);
                    // Try both namespace-aware and prefix-based attribute setting
                    if (nameEl.hasAttributeNS(WORD_ML_NS, "val")) {
                        nameEl.setAttributeNS(WORD_ML_NS, "w:val", newName);
                    } else {
                        nameEl.setAttribute("w:val", newName);
                    }
                }
            }
        }

        // Write modified XML back to the part
        try (OutputStream os = part.getOutputStream()) {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(doc), new StreamResult(os));
        }
    }
}
