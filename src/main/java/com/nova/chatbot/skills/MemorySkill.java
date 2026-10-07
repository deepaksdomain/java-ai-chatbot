package com.nova.chatbot.skills;

import com.nova.chatbot.memory.UserProfile;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remembers facts the user shares ("my name is Alex", "I like pizza")
 * and recalls them later ("what's my name?", "what do I like?").
 */
public class MemorySkill extends Skill {
    private static final Pattern SET_NAME = Pattern.compile(
            "\\b(?:my name is|my name's|call me)\\s+([a-z][a-z'-]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GET_NAME = Pattern.compile(
            "\\b(?:what(?:'s| is) my name|whats my name|who am i|do you (?:know|remember) my name)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ADD_LIKE = Pattern.compile(
            "^i (?:really )?(?:like|love|enjoy)\\s+(.+?)[.!]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GET_LIKES = Pattern.compile(
            "\\bwhat do i (?:like|love|enjoy)\\b", Pattern.CASE_INSENSITIVE);

    // "I love you" or "I like it" are not facts worth remembering.
    private static final Set<String> IGNORED_LIKES = Set.of("you", "it", "that", "this", "talking to you");

    public MemorySkill() {
        super("Memory");
    }

    @Override
    protected boolean canHandle(String input) {
        return SET_NAME.matcher(input).find()
                || GET_NAME.matcher(input).find()
                || GET_LIKES.matcher(input).find()
                || extractLike(input) != null;
    }

    @Override
    protected String handle(String input, UserProfile profile) {
        Matcher nameMatcher = SET_NAME.matcher(input);
        if (nameMatcher.find()) {
            profile.setName(nameMatcher.group(1));
            return "Nice to meet you, " + profile.getDisplayName() + "! I'll remember your name.";
        }

        if (GET_NAME.matcher(input).find()) {
            return profile.getName()
                    .map(name -> "Your name is " + name + ".")
                    .orElse("You haven't told me your name yet. Try saying \"my name is ...\"");
        }

        if (GET_LIKES.matcher(input).find()) {
            List<String> likes = profile.getLikes();
            if (likes.isEmpty()) {
                return "You haven't told me what you like yet. Try \"I like pizza\".";
            }
            return "You told me you like " + joinNaturally(likes) + ".";
        }

        String like = extractLike(input);
        boolean isNew = profile.addLike(like);
        return isNew
                ? "Got it, you like " + like + ". I'll remember that!"
                : "I already know you like " + like + ".";
    }

    /** Returns the thing the user likes, or null if this isn't an "I like ..." message. */
    private String extractLike(String input) {
        Matcher matcher = ADD_LIKE.matcher(input);
        if (!matcher.matches()) {
            return null;
        }
        String thing = matcher.group(1).trim().toLowerCase();
        return IGNORED_LIKES.contains(thing) ? null : thing;
    }

    /** ["a"] -> "a", ["a","b"] -> "a and b", ["a","b","c"] -> "a, b and c" */
    private static String joinNaturally(List<String> items) {
        if (items.size() == 1) {
            return items.get(0);
        }
        String allButLast = String.join(", ", items.subList(0, items.size() - 1));
        return allButLast + " and " + items.get(items.size() - 1);
    }
}
