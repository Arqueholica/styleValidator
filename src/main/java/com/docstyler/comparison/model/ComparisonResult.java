package com.docstyler.comparison.model;

import java.util.List;

public record ComparisonResult(
    String documentId,
    String profileId,
    int totalStyles,
    int matchedCount,
    int closeMatchCount,
    int unknownCount,
    List<StyleMatch> matches
) {
    public record StyleMatch(
        String documentStyle,
        String matchedApprovedStyle,  // null if unknown
        MatchType matchType,
        double confidence,            // 0.0 to 1.0
        List<Suggestion> suggestions  // top 3 closest for CLOSE_MATCH or UNKNOWN
    ) {}

    public record Suggestion(
        String approvedStyle,
        double score,
        String reason    // "exact_substring", "word_overlap", "levenshtein"
    ) {}

    public enum MatchType {
        EXACT,       // exact match in profile
        CLOSE_MATCH, // high confidence match found
        UNKNOWN      // no good match
    }
}
