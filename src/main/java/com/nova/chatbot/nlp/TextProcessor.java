package com.nova.chatbot.nlp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns raw text into clean "tokens" (words) the matchers can compare.
 *
 * Pipeline (a basic Natural Language Processing preprocessing step):
 *   1. lowercase              "You're SO funny!" -> "you're so funny!"
 *   2. expand contractions    "you're"           -> "you are"
 *   3. strip punctuation      "funny!"           -> "funny"
 *   4. split into words       [you, are, so, funny]
 *   5. remove stop words      filler words like "are" and "so" are dropped -> [you, funny]
 *   6. stem                   "jokes" -> "joke", "playing" -> "play"
 */
public class TextProcessor {

    // A Set gives O(1) "is this a stop word?" checks.
    // Words like "can", "do" and "you" are kept on purpose: "what can you do"
    // and "what are you" mean different things.
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "are", "am", "was", "were", "be", "been",
            "to", "of", "and", "or", "in", "on", "at", "for", "with",
            "it", "its", "this", "that", "i", "me", "my", "im",
            "please", "will", "would", "so", "just", "um", "uh", "oh");

    // Contractions are expanded so "you're" matches "you are".
    // A LinkedHashMap keeps insertion order, which matters here:
    // "can't" must be handled before the general "n't" rule.
    private static final Map<String, String> CONTRACTIONS = new LinkedHashMap<>();

    static {
        CONTRACTIONS.put("can't", "can not");
        CONTRACTIONS.put("won't", "will not");
        CONTRACTIONS.put("n't", " not");
        CONTRACTIONS.put("'re", " are");
        CONTRACTIONS.put("'m", " am");
        CONTRACTIONS.put("'ve", " have");
        CONTRACTIONS.put("'ll", " will");
        CONTRACTIONS.put("'d", " would");
        CONTRACTIONS.put("'s", "");   // "what's" -> "what", "how's" -> "how"
    }

    /** Lowercases, expands contractions, removes punctuation and splits into words (keeps stop words). */
    public List<String> tokenize(String text) {
        List<String> words = new ArrayList<>();
        if (text == null) {
            return words;
        }
        String cleaned = text.toLowerCase().replace('\u2019', '\''); // curly apostrophe -> straight
        for (Map.Entry<String, String> contraction : CONTRACTIONS.entrySet()) {
            cleaned = cleaned.replace(contraction.getKey(), contraction.getValue());
        }
        cleaned = cleaned
                .replace("'", "")                 // any leftover apostrophes
                .replaceAll("[^a-z0-9\\s]", " "); // any other symbol becomes a space
        for (String word : cleaned.split("\\s+")) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

    /** Full pipeline: tokenize, drop stop words, then stem each remaining word. */
    public List<String> process(String text) {
        List<String> tokens = new ArrayList<>();
        for (String word : tokenize(text)) {
            if (!STOP_WORDS.contains(word)) {
                tokens.add(stem(word));
            }
        }
        return tokens;
    }

    public boolean isStopWord(String word) {
        return STOP_WORDS.contains(word);
    }

    /**
     * A tiny rule-based stemmer that reduces words to a common root so that
     * "jokes" and "joke" count as the same word. Real stemmers (like Porter's)
     * have many more rules; these three cover the most common English endings.
     */
    public String stem(String word) {
        if (word.length() > 5 && word.endsWith("ing")) {
            return word.substring(0, word.length() - 3);   // "playing" -> "play"
        }
        if (word.length() > 4 && word.endsWith("ed")) {
            return word.substring(0, word.length() - 2);   // "created" -> "creat"
        }
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);   // "jokes" -> "joke"
        }
        return word;
    }
}
