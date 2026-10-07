package com.nova.chatbot.matching;

import com.nova.chatbot.knowledge.Intent;
import com.nova.chatbot.knowledge.KnowledgeBase;
import com.nova.chatbot.nlp.TextProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Matches intents using TF-IDF vectors and cosine similarity, a classic
 * information-retrieval technique used by search engines.
 *
 *   TF  (term frequency)    how often a word appears in a sentence
 *   IDF (inverse document   rare words get more weight than common ones, so
 *        frequency)          "joke" counts for more than "what"
 *   Cosine similarity        compares two word-vectors: 1.0 = same direction,
 *                            0.0 = no words in common
 *
 * Every example pattern is turned into a vector once, when the bot starts.
 * Each user message is turned into a vector and compared with all of them.
 * An intent scores as high as its best-matching pattern.
 */
public class TfIdfMatcher implements IntentMatcher {
    private static final double CONFIDENCE_THRESHOLD = 0.5;

    private final Map<String, Double> idf = new HashMap<>();
    private final List<PatternVector> patternVectors = new ArrayList<>();
    private final double unseenWordIdf; // weight for words that appear in no pattern

    /** Pre-computed vector for one example pattern. A private nested class hides this helper. */
    private static final class PatternVector {
        private final Intent intent;
        private final Map<String, Double> weights;
        private final double length;

        private PatternVector(Intent intent, Map<String, Double> weights) {
            this.intent = intent;
            this.weights = weights;
            this.length = vectorLength(weights);
        }
    }

    public TfIdfMatcher(KnowledgeBase knowledgeBase, TextProcessor processor) {
        // 1. Turn every pattern into tokens. Each pattern is one "document".
        List<Intent> owners = new ArrayList<>();
        List<List<String>> documents = new ArrayList<>();
        for (Intent intent : knowledgeBase.getIntents()) {
            for (String pattern : intent.getPatterns()) {
                List<String> tokens = processor.process(pattern);
                if (!tokens.isEmpty()) {
                    owners.add(intent);
                    documents.add(tokens);
                }
            }
        }

        // 2. Document frequency: how many patterns contain each word?
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (List<String> document : documents) {
            for (String word : new HashSet<>(document)) { // count each word once per pattern
                documentFrequency.merge(word, 1, Integer::sum);
            }
        }

        // 3. IDF = log(N / df). The +1s ("smoothing") avoid dividing by zero.
        int totalDocuments = documents.size();
        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            idf.put(entry.getKey(), Math.log((totalDocuments + 1.0) / (entry.getValue() + 1.0)) + 1.0);
        }
        this.unseenWordIdf = Math.log(totalDocuments + 1.0) + 1.0;

        // 4. Build each pattern's vector once, so answering a message is fast.
        for (int i = 0; i < documents.size(); i++) {
            patternVectors.add(new PatternVector(owners.get(i), toVector(documents.get(i))));
        }
    }

    @Override
    public String getName() {
        return "tfidf";
    }

    @Override
    public double getConfidenceThreshold() {
        return CONFIDENCE_THRESHOLD;
    }

    @Override
    public List<MatchResult> findTopMatches(List<String> tokens, int k) {
        Map<String, Double> query = toVector(tokens);
        double queryLength = vectorLength(query);
        if (queryLength == 0) {
            return List.of();
        }

        Map<Intent, Double> bestScorePerIntent = new HashMap<>();
        for (PatternVector pattern : patternVectors) {
            double similarity = cosineSimilarity(query, queryLength, pattern.weights, pattern.length);
            bestScorePerIntent.merge(pattern.intent, similarity, Math::max);
        }
        return Ranker.topK(bestScorePerIntent, k);
    }

    /** word -> (times the word appears) x (IDF weight of the word) */
    private Map<String, Double> toVector(List<String> tokens) {
        Map<String, Double> termFrequency = new HashMap<>();
        for (String token : tokens) {
            termFrequency.merge(token, 1.0, Double::sum);
        }
        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Double> entry : termFrequency.entrySet()) {
            // Unknown words get a high weight, so a message full of words the bot
            // has never seen scores low instead of being ignored.
            double weight = idf.getOrDefault(entry.getKey(), unseenWordIdf);
            vector.put(entry.getKey(), entry.getValue() * weight);
        }
        return vector;
    }

    /** cos(A, B) = (A . B) / (|A| * |B|) */
    private static double cosineSimilarity(Map<String, Double> a, double aLength,
                                           Map<String, Double> b, double bLength) {
        if (aLength == 0 || bLength == 0) {
            return 0;
        }
        double dotProduct = 0;
        for (Map.Entry<String, Double> entry : a.entrySet()) {
            Double other = b.get(entry.getKey());
            if (other != null) {
                dotProduct += entry.getValue() * other;
            }
        }
        return dotProduct / (aLength * bLength);
    }

    /** |V| = square root of the sum of squared weights */
    private static double vectorLength(Map<String, Double> vector) {
        double sumOfSquares = 0;
        for (double weight : vector.values()) {
            sumOfSquares += weight * weight;
        }
        return Math.sqrt(sumOfSquares);
    }
}
