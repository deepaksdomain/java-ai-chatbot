package com.nova.chatbot.skills;

import com.nova.chatbot.memory.UserProfile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tells the current time or date.
 *
 * The Clock is passed in through the constructor (dependency injection), so
 * tests can use a fixed clock and always get the same answer.
 */
public class TimeSkill extends Skill {
    private static final Pattern TIME_QUESTION = Pattern.compile(
            "\\b(what time|what's the time|whats the time|current time|time is it|tell me the time)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_QUESTION = Pattern.compile(
            "\\b(what day is it|what's the date|whats the date|what is the date|today's date|todays date"
                    + "|what day is today|what is today|date today)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US);

    private final Clock clock;

    public TimeSkill() {
        this(Clock.systemDefaultZone());
    }

    public TimeSkill(Clock clock) {
        super("Time");
        this.clock = clock;
    }

    @Override
    protected boolean canHandle(String input) {
        return TIME_QUESTION.matcher(input).find() || DATE_QUESTION.matcher(input).find();
    }

    @Override
    protected String handle(String input, UserProfile profile) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (TIME_QUESTION.matcher(input).find()) {
            return "It's " + now.format(TIME_FORMAT) + ".";
        }
        return "Today is " + now.format(DATE_FORMAT) + ".";
    }
}
