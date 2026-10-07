package com.nova.chatbot.memory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Stores the most recent messages in a bounded FIFO queue.
 *
 * Data structure: ArrayDeque (a double-ended queue backed by a circular array).
 * New messages are added to the back. When the queue is full, the oldest
 * message is removed from the front first. This is a "sliding window" of memory.
 *
 * Time complexity:
 *   add(...)        O(1)
 *   getRecent(n)    O(n)
 */
public class ConversationHistory {
    private final Deque<Message> messages = new ArrayDeque<>();
    private final int capacity;

    public ConversationHistory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive, got " + capacity);
        }
        this.capacity = capacity;
    }

    /** Adds a message, evicting the oldest one if the history is full. */
    public void add(Message message) {
        if (messages.size() == capacity) {
            messages.removeFirst(); // First In, First Out
        }
        messages.addLast(message);
    }

    /** Returns up to {@code n} of the most recent messages, oldest first. */
    public List<Message> getRecent(int n) {
        List<Message> recent = new ArrayList<>();
        Iterator<Message> newestFirst = messages.descendingIterator();
        while (newestFirst.hasNext() && recent.size() < n) {
            recent.add(newestFirst.next());
        }
        Collections.reverse(recent); // put them back in chronological order
        return recent;
    }

    public int size() {
        return messages.size();
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public void clear() {
        messages.clear();
    }
}
