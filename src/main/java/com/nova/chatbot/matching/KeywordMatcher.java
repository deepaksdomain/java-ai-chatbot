package com.nova.chatbot.matching;

import com.nova.chatbot.knowledge.Intent;
import com.nova.chatbot.knowledge.KnowledgeBase;
import com.nova.chatbot.nlp.TextProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Matches intents using Jaccard similarity:
 *
 *     score = (words the two sentences share) / (all distinct words in both)
 *
 * It uses an inverted index (word -> list of patterns containing that word),
 * the same idea behind search engines. Instead of scoring every pattern, the
 * bot only scores patterns that share at least one word with the user.
 */
public class KeywordMatcher implements IntentMatcher {
    private static final double CONFIDENCE_THRESHOLD = 0.4;

    private final List<Intent> patternOwners = new ArrayList<>();    // pattern id -> intent
    private final List<Set<String>> patternWords = new ArrayList<>(); // pattern id -> its words
    private final Map<String, List<Integer>> invertedIndex = new HashMap<>();

    public KeywordMatcher(KnowledgeBase knowledgeBase, TextProcessor processor) {
        for (Intent intent : knowledgeBase.getIntents()) {
            for (String pattern : intent.getPatterns()) {
                Set<String> words = new HashSet<>(processor.process(pattern));
                if (words.isEmpty()) {
                    continue;
                }
                int patternId = patternWords.size();
                patternOwners.add(intent);
                patternWords.add(words);
                for (String word : words) {
                    invertedIndex.computeIfAbsent(word, w -> new ArrayList<>()).add(patternId);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "keyword";
    }

    @Override
    public double getConfidenceThreshold() {
        return CONFIDENCE_THRESHOLD;
    }

    @Override
    public List<MatchResult> findTopMatches(List<String> tokens, int k) {
        Set<String> queryWords = new HashSet<>(tokens);

        // 1. Candidate retrieval: look up each word in the index.
        Set<Integer> candidates = new HashSet<>();
        for (String word : queryWords) {
            candidates.addAll(invertedIndex.getOrDefault(word, List.of()));
        }

        // 2. Score only the candidates.
        Map<Intent, Double> bestScorePerIntent = new HashMap<>();
        for (int patternId : candidates) {
            Set<String> words = patternWords.get(patternId);
            int shared = 0;
            for (String word : queryWords) {
                if (words.contains(word)) {
                    shared++;
                }
            }
            int union = queryWords.size() + words.size() - shared;
            double jaccard = (double) shared / union;
            bestScorePerIntent.merge(patternOwners.get(patternId), jaccard, Math::max);
        }
        return Ranker.topK(bestScorePerIntent, k);
    }

    /** Number of distinct words in the index (useful for debugging). */
    public int indexSize() {
        return invertedIndex.size();
    }
}
