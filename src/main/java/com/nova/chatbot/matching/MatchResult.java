package com.nova.chatbot.matching;

import com.nova.chatbot.knowledge.Intent;

/**
 * An intent paired with how well it matched the user's message.
 *
 * Implements Comparable so a PriorityQueue (heap) and sort() know how to
 * order results: a higher score means a better match.
 */
public final class MatchResult implements Comparable<MatchResult> {
    private final Intent intent;
    private final double score; // 0.0 = nothing in common, 1.0 = perfect match

    public MatchResult(Intent intent, double score) {
        this.intent = intent;
        this.score = score;
    }

    public Intent getIntent() {
        return intent;
    }

    public double getScore() {
        return score;
    }

    @Override
    public int compareTo(MatchResult other) {
        int byScore = Double.compare(this.score, other.score);
        if (byScore != 0) {
            return byScore;
        }
        // Tie-breaker so results are always predictable:
        // the alphabetically-first tag ranks higher.
        return other.intent.getTag().compareTo(this.intent.getTag());
    }

    @Override
    public String toString() {
        return String.format("%s (%.2f)", intent.getTag(), score);
    }
}
