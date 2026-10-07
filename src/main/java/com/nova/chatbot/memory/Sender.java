package com.nova.chatbot.memory;

/**
 * Who sent a message.
 *
 * An enum limits the value to a fixed set of constants, so a message can never
 * have an invalid sender like "Bob" or null-by-typo.
 */
public enum Sender {
    USER("You"),
    BOT("Nova");

    private final String displayName;

    Sender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
