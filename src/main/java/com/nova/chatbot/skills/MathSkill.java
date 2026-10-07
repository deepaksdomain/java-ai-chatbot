package com.nova.chatbot.skills;

import com.nova.chatbot.memory.UserProfile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Answers arithmetic questions such as "what is 12 * (3 + 4)?" or "2^10".
 * Inheritance: MathSkill IS-A Skill and fills in canHandle() and handle().
 */
public class MathSkill extends Skill {

    // Optional lead-in words, then only numbers, operators, parentheses and spaces.
    private static final Pattern MATH_QUESTION = Pattern.compile(
            "^(?:what\\s+is|what's|whats|calculate|calc|compute|solve|evaluate)?\\s*([-+*/%^().\\d\\s]+?)\\s*[?=]?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_OPERATOR = Pattern.compile("[-+*/%^]");

    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

    public MathSkill() {
        super("Math");
    }

    @Override
    protected boolean canHandle(String input) {
        Matcher matcher = MATH_QUESTION.matcher(input);
        if (!matcher.matches()) {
            return false;
        }
        String expression = matcher.group(1);
        return HAS_DIGIT.matcher(expression).find() && HAS_OPERATOR.matcher(expression).find();
    }

    @Override
    protected String handle(String input, UserProfile profile) {
        Matcher matcher = MATH_QUESTION.matcher(input);
        matcher.matches();
        String expression = matcher.group(1).trim();
        try {
            double result = evaluator.evaluate(expression);
            return expression + " = " + ExpressionEvaluator.format(result);
        } catch (ArithmeticException | IllegalArgumentException e) {
            return "I couldn't calculate that: " + e.getMessage() + ".";
        }
    }
}
