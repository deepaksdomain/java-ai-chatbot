package com.nova.chatbot.knowledge;

import java.util.List;
import java.util.Objects;

/**
 * An intent is one topic the bot understands, e.g. "greeting" or "joke".
 *
 *   tag        unique id for the topic
 *   patterns   example sentences a user might type
 *   responses  replies the bot can give (one is picked at random)
 *
 * Immutable: the lists are copied with List.copyOf, so nobody can change an
 * intent after it is built.
 */
public final class Intent {
    private final String tag;
    private final List<String> patterns;
    private final List<String> responses;

    public Intent(String tag, List<String> patterns, List<String> responses) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Intent tag cannot be blank");
        }
        if (patterns == null || patterns.isEmpty()) {
            throw new IllegalArgumentException("Intent '" + tag + "' needs at least one pattern");
        }
        if (responses == null || responses.isEmpty()) {
            throw new IllegalArgumentException("Intent '" + tag + "' needs at least one response");
        }
        this.tag = tag.trim();
        this.patterns = List.copyOf(patterns);
        this.responses = List.copyOf(responses);
    }

    public String getTag() {
        return tag;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public List<String> getResponses() {
        return responses;
    }

    /** A sample sentence for this intent, used in "Did you mean...?" suggestions. */
    public String getExamplePattern() {
        return patterns.get(0);
    }

    // Two intents are equal if they have the same tag. equals() and hashCode()
    // must agree so Intent works correctly as a HashMap key.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Intent)) {
            return false;
        }
        return tag.equals(((Intent) other).tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag);
    }

    @Override
    public String toString() {
        return "Intent{" + tag + ", " + patterns.size() + " patterns, " + responses.size() + " responses}";
    }
}
