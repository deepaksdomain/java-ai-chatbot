package com.nova.chatbot.memory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * One line of the conversation.
 *
 * This class is immutable: every field is private and final, and there are no
 * setters. Once a message is created it can never change, which makes it safe
 * to share and store in collections.
 */
public final class Message {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Sender sender;
    private final String text;
    private final LocalTime timestamp;

    public Message(Sender sender, String text) {
        this(sender, text, LocalTime.now());
    }

    public Message(Sender sender, String text, LocalTime timestamp) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.text = Objects.requireNonNull(text, "text");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public Sender getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public LocalTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp.format(TIME_FORMAT) + "] " + sender.getDisplayName() + ": " + text;
    }
}
