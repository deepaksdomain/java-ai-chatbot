package com.nova.chatbot.matching;

import com.nova.chatbot.knowledge.Intent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Picks the top-k best scores using a min-heap (Java's PriorityQueue).
 *
 * How it works: the heap never holds more than k results, and its smallest
 * score is always on top. When a new result pushes the size past k, the
 * smallest is removed. What is left at the end are the k largest.
 *
 * Time O(n log k), which beats sorting everything, O(n log n), when k is small.
 */
public final class Ranker {

    private Ranker() {
        // Utility class: only static methods, so no one should create an instance.
    }

    public static List<MatchResult> topK(Map<Intent, Double> scores, int k) {
        if (k <= 0) {
            return List.of();
        }
        PriorityQueue<MatchResult> minHeap = new PriorityQueue<>(); // uses MatchResult.compareTo

        for (Map.Entry<Intent, Double> entry : scores.entrySet()) {
            if (entry.getValue() <= 0) {
                continue; // no overlap at all, not worth ranking
            }
            minHeap.offer(new MatchResult(entry.getKey(), entry.getValue()));
            if (minHeap.size() > k) {
                minHeap.poll(); // drop the weakest result
            }
        }

        List<MatchResult> best = new ArrayList<>(minHeap);
        best.sort(Comparator.reverseOrder()); // highest score first
        return best;
    }
}
