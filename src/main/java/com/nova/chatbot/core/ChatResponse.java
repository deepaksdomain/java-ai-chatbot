package com.nova.chatbot.core;

import com.nova.chatbot.matching.MatchResult;

import java.util.List;
import java.util.Map;

/**
 * Everything the bot decided about one reply: the text plus the "why".
 *
 * Returning an object instead of a plain String separates the bot's logic
 * from how the console displays it (Separation of Concerns). The console can
 * print just the text, or the debug details too.
 *
 * Objects are created through static factory methods (fromSkill, fromIntent,
 * fallback) whose names explain what kind of response is being built.
 */
public final class ChatResponse {

    /** Where the answer came from. */
    public enum Source { SKILL, INTENT, FALLBACK }

    private final String text;
    private final Source source;
    private final String handledBy;          // skill name or intent tag ("" for fallback)
    private final double confidence;         // 1.0 for skills, match score for intents
    private final List<MatchResult> topMatches;
    private final Map<String, String> corrections; // typo -> fixed word

    private ChatResponse(String text, Source source, String handledBy, double confidence,
                         List<MatchResult> topMatches, Map<String, String> corrections) {
        this.text = text;
        this.source = source;
        this.handledBy = handledBy;
        this.confidence = confidence;
        this.topMatches = List.copyOf(topMatches);
        this.corrections = Map.copyOf(corrections);
    }

    public static ChatResponse fromSkill(String text, String skillName) {
        return new ChatResponse(text, Source.SKILL, skillName, 1.0, List.of(), Map.of());
    }

    public static ChatResponse fromIntent(String text, String intentTag, double confidence,
                                          List<MatchResult> topMatches, Map<String, String> corrections) {
        return new ChatResponse(text, Source.INTENT, intentTag, confidence, topMatches, corrections);
    }

    public static ChatResponse fallback(String text, List<MatchResult> topMatches, Map<String, String> corrections) {
        double bestScore = topMatches.isEmpty() ? 0.0 : topMatches.get(0).getScore();
        return new ChatResponse(text, Source.FALLBACK, "", bestScore, topMatches, corrections);
    }

    public String getText() {
        return text;
    }

    public Source getSource() {
        return source;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public double getConfidence() {
        return confidence;
    }

    public List<MatchResult> getTopMatches() {
        return topMatches;
    }

    public Map<String, String> getCorrections() {
        return corrections;
    }

    /** True when the user said goodbye, so the console knows to end the chat. */
    public boolean isGoodbye() {
        return source == Source.INTENT && ChatBot.GOODBYE_TAG.equals(handledBy);
    }
}
