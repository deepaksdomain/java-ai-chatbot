package com.nova.chatbot.skills;

import com.nova.chatbot.memory.UserProfile;

import java.util.Optional;

/**
 * A Skill is a special ability that runs BEFORE normal intent matching,
 * for things a fixed list of replies can't do: math, the time, remembering facts.
 *
 * Skills form a Chain of Responsibility: the bot asks each skill in order,
 * and the first one that can handle the message answers it.
 *
 * Why an abstract class instead of an interface? Every skill shares the same
 * name field and the same respond() workflow, so that shared code lives here
 * once. Subclasses only fill in the two steps that differ (Template Method pattern).
 */
public abstract class Skill {
    private final String name;

    protected Skill(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    /**
     * Template method: the same steps for every skill. It is final so
     * subclasses can't change the workflow, only the steps inside it.
     */
    public final Optional<String> tryRespond(String input, UserProfile profile) {
        String cleaned = input == null ? "" : input.trim();
        if (cleaned.isEmpty() || !canHandle(cleaned)) {
            return Optional.empty();
        }
        return Optional.of(handle(cleaned, profile));
    }

    /** Step 1: does this skill recognise the message? */
    protected abstract boolean canHandle(String input);

    /** Step 2: build the reply. Only called when canHandle returned true. */
    protected abstract String handle(String input, UserProfile profile);
}
