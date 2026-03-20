package com.docstyler.reporting.service;

import com.docstyler.comparison.model.ComparisonResult;
import com.docstyler.comparison.model.ComparisonResult.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    public String generateHtmlReport(ComparisonResult result, String documentName) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<title>Style Validation Report - ").append(esc(documentName)).append("</title>");
        html.append("<style>");
        html.append("body{font-family:Arial,sans-serif;max-width:900px;margin:0 auto;padding:20px;color:#333}");
        html.append("h1{color:#1a1a2e;border-bottom:2px solid #16213e;padding-bottom:10px}");
        html.append("h2{color:#16213e;margin-top:30px}");
        html.append(".summary{display:grid;grid-template-columns:repeat(4,1fr);gap:15px;margin:20px 0}");
        html.append(".card{padding:15px;border-radius:8px;text-align:center}");
        html.append(".card h3{margin:0;font-size:28px}.card p{margin:5px 0 0;font-size:12px;text-transform:uppercase}");
        html.append(".total{background:#e8eaf6;color:#283593}");
        html.append(".matched{background:#e8f5e9;color:#2e7d32}");
        html.append(".close{background:#fff3e0;color:#e65100}");
        html.append(".unknown{background:#ffebee;color:#c62828}");
        html.append("table{width:100%;border-collapse:collapse;margin:15px 0}");
        html.append("th,td{padding:10px 12px;text-align:left;border-bottom:1px solid #e0e0e0}");
        html.append("th{background:#f5f5f5;font-weight:600}");
        html.append("tr:hover{background:#fafafa}");
        html.append(".badge{padding:3px 8px;border-radius:12px;font-size:11px;font-weight:600}");
        html.append(".badge-exact{background:#c8e6c9;color:#2e7d32}");
        html.append(".badge-close{background:#ffe0b2;color:#e65100}");
        html.append(".badge-unknown{background:#ffcdd2;color:#c62828}");
        html.append(".suggestion{font-size:12px;color:#666;margin-top:4px}");
        html.append(".footer{margin-top:40px;padding-top:15px;border-top:1px solid #e0e0e0;color:#999;font-size:12px}");
        html.append("</style></head><body>");

        // Header
        html.append("<h1>Style Validation Report</h1>");
        html.append("<p><strong>Document:</strong> ").append(esc(documentName)).append("</p>");
        html.append("<p><strong>Generated:</strong> ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>");

        // Summary cards
        html.append("<div class='summary'>");
        html.append("<div class='card total'><h3>").append(result.totalStyles()).append("</h3><p>Total Styles</p></div>");
        html.append("<div class='card matched'><h3>").append(result.matchedCount()).append("</h3><p>Matched</p></div>");
        html.append("<div class='card close'><h3>").append(result.closeMatchCount()).append("</h3><p>Close Matches</p></div>");
        html.append("<div class='card unknown'><h3>").append(result.unknownCount()).append("</h3><p>Unknown</p></div>");
        html.append("</div>");

        // Detailed table
        html.append("<h2>Style Details</h2>");
        html.append("<table><thead><tr><th>Document Style</th><th>Status</th><th>Matched To</th><th>Confidence</th><th>Suggestions</th></tr></thead><tbody>");

        for (StyleMatch match : result.matches()) {
            html.append("<tr>");
            html.append("<td><strong>").append(esc(match.documentStyle())).append("</strong></td>");

            String badgeClass = switch (match.matchType()) {
                case EXACT -> "badge-exact";
                case CLOSE_MATCH -> "badge-close";
                case UNKNOWN -> "badge-unknown";
            };
            html.append("<td><span class='badge ").append(badgeClass).append("'>")
                .append(match.matchType().name().replace("_", " ")).append("</span></td>");

            html.append("<td>").append(match.matchedApprovedStyle() != null ? esc(match.matchedApprovedStyle()) : "-").append("</td>");
            html.append("<td>").append(match.matchType() == MatchType.EXACT ? "100%" : Math.round(match.confidence() * 100) + "%").append("</td>");

            // Suggestions column
            html.append("<td>");
            if (!match.suggestions().isEmpty()) {
                for (Suggestion s : match.suggestions()) {
                    html.append("<div class='suggestion'>").append(esc(s.approvedStyle()))
                        .append(" (").append(Math.round(s.score() * 100)).append("% - ").append(s.reason()).append(")</div>");
                }
            } else {
                html.append("-");
            }
            html.append("</td></tr>");
        }

        html.append("</tbody></table>");

        // Footer
        html.append("<div class='footer'>Generated by DocStyler v2.0</div>");
        html.append("</body></html>");

        return html.toString();
    }

    public String generateCsvReport(ComparisonResult result) {
        StringBuilder csv = new StringBuilder();
        csv.append("Document Style,Match Type,Matched To,Confidence,Top Suggestion\n");
        for (StyleMatch match : result.matches()) {
            csv.append(csvEsc(match.documentStyle())).append(",");
            csv.append(match.matchType().name()).append(",");
            csv.append(csvEsc(match.matchedApprovedStyle() != null ? match.matchedApprovedStyle() : "")).append(",");
            csv.append(Math.round(match.confidence() * 100)).append("%,");
            csv.append(!match.suggestions().isEmpty() ? csvEsc(match.suggestions().get(0).approvedStyle()) : "").append("\n");
        }
        return csv.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String csvEsc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
