package com.nova.chatbot.knowledge;

/**
 * Thrown when the intents file is missing or badly formatted.
 *
 * This is a custom checked exception (it extends Exception, not
 * RuntimeException), so the compiler forces callers to handle it.
 */
public class KnowledgeBaseException extends Exception {
    private static final long serialVersionUID = 1L;

    public KnowledgeBaseException(String message) {
        super(message);
    }

    public KnowledgeBaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
