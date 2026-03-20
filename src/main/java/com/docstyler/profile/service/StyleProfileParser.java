package com.docstyler.profile.service;

import com.docstyler.profile.model.ApprovedStyle;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class StyleProfileParser {

    public List<ApprovedStyle> parse(String xml) {
        return parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    public List<ApprovedStyle> parse(InputStream is) {
        List<ApprovedStyle> result = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);

            NodeList relations = doc.getElementsByTagName("relation");
            for (int i = 0; i < relations.getLength(); i++) {
                Element rel = (Element) relations.item(i);
                String element = getTextContent(rel, "element");
                String styleName = getTextContent(rel, "styleName");
                Element htmlEl = getFirstElement(rel, "resultedHTML");
                String resultedHTML = htmlEl != null ? htmlEl.getTextContent().trim() : "";
                boolean fresh = htmlEl != null && "true".equals(htmlEl.getAttribute("fresh"));

                if (styleName != null && !styleName.isBlank()) {
                    result.add(new ApprovedStyle(styleName, element, resultedHTML, fresh));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse style profile XML: " + e.getMessage(), e);
        }
        return result;
    }

    /** Returns just the unique set of approved style names from the XML */
    public Set<String> extractApprovedNames(String xml) {
        Set<String> names = new LinkedHashSet<>();
        for (ApprovedStyle style : parse(xml)) {
            names.add(style.styleName());
        }
        return names;
    }

    private String getTextContent(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        return nl.getLength() > 0 ? nl.item(0).getTextContent().trim() : null;
    }

    private Element getFirstElement(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        return nl.getLength() > 0 ? (Element) nl.item(0) : null;
    }
}
