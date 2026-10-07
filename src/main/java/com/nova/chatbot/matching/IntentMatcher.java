package com.nova.chatbot.matching;

import java.util.List;

/**
 * Strategy interface: anything that can score how well the user's words
 * match each intent.
 *
 * ChatBot depends on this interface, not on a specific class, so different
 * matching algorithms can be plugged in or swapped while the bot is running.
 * This is the Strategy design pattern, and it relies on polymorphism.
 */
public interface IntentMatcher {

    /** Short name shown to the user, e.g. "tfidf". */
    String getName();

    /** Minimum score (0.0 to 1.0) needed before the bot trusts the best match. */
    double getConfidenceThreshold();

    /**
     * Returns up to {@code k} intents with the highest scores, best first.
     * The tokens have already been cleaned by the TextProcessor.
     */
    List<MatchResult> findTopMatches(List<String> tokens, int k);
}
