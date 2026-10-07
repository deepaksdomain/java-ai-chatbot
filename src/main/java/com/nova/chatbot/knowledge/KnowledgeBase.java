package com.nova.chatbot.knowledge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything the bot knows: a collection of intents.
 *
 * Two data structures hold the same intents for two different jobs:
 *   ArrayList  keeps them in file order, for looping over all of them
 *   HashMap    finds one by its tag in O(1) average time
 */
public class KnowledgeBase {
    private static final String PATTERNS_PREFIX = "patterns:";
    private static final String RESPONSES_PREFIX = "responses:";

    private final List<Intent> intents = new ArrayList<>();
    private final Map<String, Intent> intentsByTag = new HashMap<>();

    public void addIntent(Intent intent) {
        if (intentsByTag.containsKey(intent.getTag())) {
            throw new IllegalArgumentException("Duplicate intent tag: " + intent.getTag());
        }
        intents.add(intent);
        intentsByTag.put(intent.getTag(), intent);
    }

    public Optional<Intent> findByTag(String tag) {
        return Optional.ofNullable(intentsByTag.get(tag));
    }

    /** Read-only view: callers can look but not add or remove. */
    public List<Intent> getIntents() {
        return Collections.unmodifiableList(intents);
    }

    public int size() {
        return intents.size();
    }

    // ------------------------------------------------------------------
    // Loading from a text file (static factory methods)
    // ------------------------------------------------------------------

    /** Reads and parses an intents file such as data/intents.txt. */
    public static KnowledgeBase loadFromFile(Path path) throws KnowledgeBaseException {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new KnowledgeBaseException("Could not read intents file: " + path.toAbsolutePath(), e);
        }
        return parse(lines);
    }

    /**
     * Parses this simple format:
     *
     *   # comment
     *   [greeting]
     *   patterns: hello | hi | hey
     *   responses: Hello! | Hi there!
     */
    public static KnowledgeBase parse(List<String> lines) throws KnowledgeBaseException {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        String currentTag = null;
        int currentTagLine = 0;
        List<String> patterns = new ArrayList<>();
        List<String> responses = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            int lineNumber = i + 1;

            if (line.isEmpty() || line.startsWith("#")) {
                continue; // skip blank lines and comments
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                // A new intent starts, so save the one we were building.
                if (currentTag != null) {
                    knowledgeBase.addParsedIntent(currentTag, patterns, responses, currentTagLine);
                }
                currentTag = line.substring(1, line.length() - 1).trim();
                currentTagLine = lineNumber;
                patterns = new ArrayList<>();
                responses = new ArrayList<>();
            } else if (line.startsWith(PATTERNS_PREFIX)) {
                requireTag(currentTag, lineNumber);
                patterns.addAll(splitOptions(line.substring(PATTERNS_PREFIX.length())));
            } else if (line.startsWith(RESPONSES_PREFIX)) {
                requireTag(currentTag, lineNumber);
                responses.addAll(splitOptions(line.substring(RESPONSES_PREFIX.length())));
            } else {
                throw new KnowledgeBaseException("Line " + lineNumber + ": unrecognized line \"" + line + "\"");
            }
        }

        if (currentTag != null) {
            knowledgeBase.addParsedIntent(currentTag, patterns, responses, currentTagLine);
        }
        if (knowledgeBase.size() == 0) {
            throw new KnowledgeBaseException("The intents file does not contain any intents");
        }
        return knowledgeBase;
    }

    private void addParsedIntent(String tag, List<String> patterns, List<String> responses, int lineNumber)
            throws KnowledgeBaseException {
        try {
            addIntent(new Intent(tag, patterns, responses));
        } catch (IllegalArgumentException e) {
            // Convert to our checked exception and add the line number to help the user fix the file.
            throw new KnowledgeBaseException("Line " + lineNumber + ": " + e.getMessage(), e);
        }
    }

    private static void requireTag(String tag, int lineNumber) throws KnowledgeBaseException {
        if (tag == null) {
            throw new KnowledgeBaseException("Line " + lineNumber + ": found patterns/responses before any [tag]");
        }
    }

    /** Splits "a | b | c" into ["a", "b", "c"], ignoring empty pieces. */
    private static List<String> splitOptions(String text) {
        List<String> options = new ArrayList<>();
        for (String piece : text.split("\\|")) {
            String trimmed = piece.trim();
            if (!trimmed.isEmpty()) {
                options.add(trimmed);
            }
        }
        return options;
    }
}
