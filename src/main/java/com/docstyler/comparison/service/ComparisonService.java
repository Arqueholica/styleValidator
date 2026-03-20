package com.docstyler.comparison.service;

import com.docstyler.comparison.model.ComparisonResult;
import com.docstyler.comparison.model.ComparisonResult.*;
import com.docstyler.extraction.model.DocumentStructure;
import com.docstyler.extraction.model.ExtractedStyle;
import com.docstyler.mapping.service.MappingService;
import com.docstyler.profile.service.ProfileService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComparisonService {

    private static final double CLOSE_MATCH_THRESHOLD = 0.5;
    private static final int MAX_SUGGESTIONS = 3;

    private final ProfileService profileService;
    private final MappingService mappingService;

    public ComparisonService(ProfileService profileService, MappingService mappingService) {
        this.profileService = profileService;
        this.mappingService = mappingService;
    }

    public ComparisonResult compare(String documentId, DocumentStructure structure, String profileId) {
        Set<String> approvedNames = profileService.getApprovedNames(profileId);
        List<String> approvedList = new ArrayList<>(approvedNames);

        // Get existing mappings for this profile
        Map<String, String> existingMappings = mappingService.getMappingsForProfile(profileId);

        List<StyleMatch> matches = new ArrayList<>();
        int matched = 0, closeMatch = 0, unknown = 0;

        for (ExtractedStyle style : structure.styles()) {
            String docStyle = style.name();

            // Skip "Normal" and other default styles that don't carry meaningful info
            if ("Normal".equals(docStyle)) {
                matches.add(new StyleMatch(docStyle, docStyle, MatchType.EXACT, 1.0, List.of()));
                matched++;
                continue;
            }

            // Check existing persistent mappings first
            if (existingMappings.containsKey(docStyle.toLowerCase())) {
                String mappedTo = existingMappings.get(docStyle.toLowerCase());
                matches.add(new StyleMatch(docStyle, mappedTo, MatchType.EXACT, 1.0, List.of()));
                matched++;
                continue;
            }

            // Pass 0: Exact match
            if (approvedNames.contains(docStyle)) {
                matches.add(new StyleMatch(docStyle, docStyle, MatchType.EXACT, 1.0, List.of()));
                matched++;
                continue;
            }

            // Case-insensitive exact match
            Optional<String> caseMatch = approvedNames.stream()
                .filter(a -> a.equalsIgnoreCase(docStyle)).findFirst();
            if (caseMatch.isPresent()) {
                matches.add(new StyleMatch(docStyle, caseMatch.get(), MatchType.EXACT, 0.99, List.of()));
                matched++;
                continue;
            }

            // Run 3-pass proximity search
            List<Suggestion> suggestions = findClosestMatches(docStyle, approvedList);

            if (!suggestions.isEmpty() && suggestions.get(0).score() >= CLOSE_MATCH_THRESHOLD) {
                matches.add(new StyleMatch(docStyle, suggestions.get(0).approvedStyle(),
                    MatchType.CLOSE_MATCH, suggestions.get(0).score(), suggestions));
                closeMatch++;
            } else {
                matches.add(new StyleMatch(docStyle, null, MatchType.UNKNOWN, 0.0, suggestions));
                unknown++;
            }
        }

        return new ComparisonResult(documentId, profileId, structure.styles().size(),
            matched, closeMatch, unknown, matches);
    }

    /**
     * 3-pass closest match: exact substring -> word overlap -> Levenshtein distance
     */
    private List<Suggestion> findClosestMatches(String target, List<String> approved) {
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, String> reasons = new LinkedHashMap<>();
        String targetLower = target.toLowerCase();

        for (String app : approved) {
            String appLower = app.toLowerCase();
            double bestScore = 0;
            String bestReason = "";

            // Pass 1: Exact substring match
            double substringScore = substringScore(targetLower, appLower);
            if (substringScore > bestScore) { bestScore = substringScore; bestReason = "exact_substring"; }

            // Pass 2: Word overlap
            double wordScore = wordOverlapScore(targetLower, appLower);
            if (wordScore > bestScore) { bestScore = wordScore; bestReason = "word_overlap"; }

            // Pass 3: Levenshtein distance
            double levScore = levenshteinScore(targetLower, appLower);
            if (levScore > bestScore) { bestScore = levScore; bestReason = "levenshtein"; }

            if (bestScore > 0.2) {
                scores.put(app, bestScore);
                reasons.put(app, bestReason);
            }
        }

        return scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(MAX_SUGGESTIONS)
            .map(e -> new Suggestion(e.getKey(), Math.round(e.getValue() * 100.0) / 100.0, reasons.get(e.getKey())))
            .collect(Collectors.toList());
    }

    /** Score based on substring containment */
    private double substringScore(String target, String approved) {
        if (target.contains(approved) || approved.contains(target)) {
            double shorter = Math.min(target.length(), approved.length());
            double longer = Math.max(target.length(), approved.length());
            return 0.7 + 0.3 * (shorter / longer);
        }
        return 0;
    }

    /** Score based on word overlap */
    private double wordOverlapScore(String target, String approved) {
        Set<String> targetWords = new HashSet<>(Arrays.asList(target.split("[\\s_\\-.]+")));
        Set<String> approvedWords = new HashSet<>(Arrays.asList(approved.split("[\\s_\\-.]+")));
        if (targetWords.isEmpty() || approvedWords.isEmpty()) return 0;

        long common = targetWords.stream().filter(approvedWords::contains).count();
        double union = new HashSet<>() {{ addAll(targetWords); addAll(approvedWords); }}.size();
        return common / union; // Jaccard similarity
    }

    /** Score based on normalized Levenshtein distance */
    private double levenshteinScore(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - ((double) distance / maxLen);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[] prev = new int[s2.length() + 1];
        int[] curr = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) prev[j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev; prev = curr; curr = temp;
        }
        return prev[s2.length()];
    }
}
