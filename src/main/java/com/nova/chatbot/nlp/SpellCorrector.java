package com.nova.chatbot.nlp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fixes small typos by finding the closest word the bot already knows.
 *
 * Algorithm: edit distance, solved with dynamic programming.
 * Data structures:
 *   HashSet  vocabulary, for O(1) "do I know this word?" checks
 *   HashMap  cache of corrections already computed (memoization)
 */
public class SpellCorrector {
    private static final int MIN_WORD_LENGTH = 4; // short words are too ambiguous to correct

    private final Set<String> vocabulary;
    private final Map<String, String> cache = new HashMap<>();

    public SpellCorrector(Collection<String> knownWords) {
        this.vocabulary = new HashSet<>(knownWords);
    }

    public boolean isKnown(String word) {
        return vocabulary.contains(word);
    }

    public int vocabularySize() {
        return vocabulary.size();
    }

    /** Returns the corrected word, or the original word if nothing is close enough. */
    public String correct(String word) {
        if (vocabulary.contains(word) || word.length() < MIN_WORD_LENGTH || containsDigit(word)) {
            return word;
        }
        // computeIfAbsent only runs findClosest the first time we see this typo.
        return cache.computeIfAbsent(word, this::findClosest);
    }

    private String findClosest(String word) {
        int maxDistance = word.length() <= 5 ? 1 : 2; // allow more mistakes in longer words
        String best = word;
        int bestDistance = maxDistance + 1;

        for (String candidate : vocabulary) {
            // Pruning: if the lengths differ by more than maxDistance, the
            // distance must be too big, so skip the expensive calculation.
            if (Math.abs(candidate.length() - word.length()) > maxDistance) {
                continue;
            }
            int distance = editDistance(word, candidate);
            boolean closer = distance < bestDistance;
            boolean sameButAlphabeticallyFirst = distance == bestDistance && candidate.compareTo(best) < 0;
            if (closer || sameButAlphabeticallyFirst) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return bestDistance <= maxDistance ? best : word;
    }

    /**
     * Minimum number of single-character edits (insert, delete, replace, or
     * swap two neighbouring letters) needed to turn {@code a} into {@code b}.
     *
     * Dynamic programming: dp[i][j] = distance between the first i letters of a
     * and the first j letters of b. Each cell is built from cells already solved.
     *
     * Time O(n * m), space O(n * m).
     */
    public static int editDistance(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 0; i <= n; i++) {
            dp[i][0] = i; // delete all i letters
        }
        for (int j = 0; j <= m; j++) {
            dp[0][j] = j; // insert all j letters
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int delete = dp[i - 1][j] + 1;
                int insert = dp[i][j - 1] + 1;
                int replace = dp[i - 1][j - 1] + cost;
                dp[i][j] = Math.min(Math.min(delete, insert), replace);

                // Swapped neighbours, e.g. "jkoe" -> "joke", count as one edit.
                boolean swapped = i > 1 && j > 1
                        && a.charAt(i - 1) == b.charAt(j - 2)
                        && a.charAt(i - 2) == b.charAt(j - 1);
                if (swapped) {
                    dp[i][j] = Math.min(dp[i][j], dp[i - 2][j - 2] + 1);
                }
            }
        }
        return dp[n][m];
    }

    private static boolean containsDigit(String word) {
        for (char c : word.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }
}
