package com.nova.chatbot.memory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What the bot has learned about the user during this chat (its short-term memory).
 *
 * Encapsulation: fields are private and can only change through methods that
 * validate the input. Callers get copies of the data, never the internal set.
 */
public class UserProfile {
    private static final String DEFAULT_NAME = "friend";

    private String name; // null until the user tells us

    // LinkedHashSet: no duplicates (like a HashSet) AND remembers insertion order.
    private final Set<String> likes = new LinkedHashSet<>();

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /** The name to use in replies: the user's name, or "friend" if we don't know it yet. */
    public String getDisplayName() {
        return name == null ? DEFAULT_NAME : name;
    }

    public void setName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        String trimmed = newName.trim();
        this.name = Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }

    /** Remembers something the user likes. Returns false if we already knew it. */
    public boolean addLike(String thing) {
        if (thing == null || thing.isBlank()) {
            throw new IllegalArgumentException("Like cannot be blank");
        }
        return likes.add(thing.trim().toLowerCase());
    }

    /** Returns a read-only copy so outside code cannot modify our internal set. */
    public List<String> getLikes() {
        return List.copyOf(likes);
    }
}
