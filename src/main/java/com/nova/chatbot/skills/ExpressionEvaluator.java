package com.nova.chatbot.skills;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Evaluates math such as "3 + 4 * (2 - 1)" using stacks and Dijkstra's
 * Shunting-Yard algorithm.
 *
 *   Step 1  tokenize     "3+4*2"       ->  [3, +, 4, *, 2]
 *   Step 2  to postfix   [3, +, 4, *, 2] -> [3, 4, 2, *, +]
 *           An operator stack holds operators until we know their order,
 *           which handles precedence (* before +) and parentheses.
 *   Step 3  evaluate     Push numbers on a stack. For each operator, pop two
 *           numbers, apply it, and push the result back.
 *
 * Supports + - * / % ^ (power), parentheses, decimals and negative numbers.
 * Time O(n) where n is the number of tokens.
 */
public class ExpressionEvaluator {
    private static final String NEGATE = "neg"; // unary minus, as in "-3" or "2 * -3"
    private static final String OPERATORS = "+-*/%^";

    public double evaluate(String expression) {
        List<String> tokens = tokenize(expression);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("the expression is empty");
        }
        return evaluatePostfix(toPostfix(tokens));
    }

    // ---------------- Step 1: tokenize ----------------

    List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < expression.length()) {
            char c = expression.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isDigit(c) || c == '.') {
                int start = i;
                while (i < expression.length()
                        && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    i++;
                }
                tokens.add(expression.substring(start, i));
            } else if (c == '(' || c == ')') {
                tokens.add(String.valueOf(c));
                i++;
            } else if (OPERATORS.indexOf(c) >= 0) {
                boolean unary = isUnaryPosition(tokens);
                if (c == '-' && unary) {
                    tokens.add(NEGATE);
                } else if (!(c == '+' && unary)) { // a leading "+" changes nothing, so skip it
                    tokens.add(String.valueOf(c));
                }
                i++;
            } else {
                throw new IllegalArgumentException("unexpected character '" + c + "'");
            }
        }
        return tokens;
    }

    /** A minus sign is unary if it comes first, after "(", or after another operator. */
    private boolean isUnaryPosition(List<String> tokens) {
        if (tokens.isEmpty()) {
            return true;
        }
        String previous = tokens.get(tokens.size() - 1);
        return previous.equals("(") || isOperator(previous);
    }

    // ---------------- Step 2: infix -> postfix ----------------

    List<String> toPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Deque<String> operatorStack = new ArrayDeque<>(); // ArrayDeque used as a stack (push/pop/peek)

        for (String token : tokens) {
            if (isNumber(token)) {
                output.add(token);
            } else if (token.equals(NEGATE) || token.equals("(")) {
                operatorStack.push(token);
            } else if (token.equals(")")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    output.add(operatorStack.pop());
                }
                if (operatorStack.isEmpty()) {
                    throw new IllegalArgumentException("mismatched parentheses");
                }
                operatorStack.pop(); // throw away the "("
            } else {
                // Binary operator: first pop operators that must run before it.
                while (!operatorStack.isEmpty() && shouldPopBefore(operatorStack.peek(), token)) {
                    output.add(operatorStack.pop());
                }
                operatorStack.push(token);
            }
        }

        while (!operatorStack.isEmpty()) {
            String operator = operatorStack.pop();
            if (operator.equals("(")) {
                throw new IllegalArgumentException("mismatched parentheses");
            }
            output.add(operator);
        }
        return output;
    }

    private boolean shouldPopBefore(String top, String incoming) {
        if (top.equals("(")) {
            return false;
        }
        int topPrecedence = precedence(top);
        int incomingPrecedence = precedence(incoming);
        return topPrecedence > incomingPrecedence
                || (topPrecedence == incomingPrecedence && !isRightAssociative(incoming));
    }

    private int precedence(String operator) {
        switch (operator) {
            case "+":
            case "-":
                return 1;
            case "*":
            case "/":
            case "%":
                return 2;
            case NEGATE:
                return 3;
            case "^":
                return 4;
            default:
                throw new IllegalArgumentException("unknown operator " + operator);
        }
    }

    /** 2 ^ 3 ^ 2 means 2 ^ (3 ^ 2), so power groups from the right. */
    private boolean isRightAssociative(String operator) {
        return operator.equals("^");
    }

    // ---------------- Step 3: evaluate postfix ----------------

    double evaluatePostfix(List<String> postfix) {
        Deque<Double> stack = new ArrayDeque<>();
        for (String token : postfix) {
            if (isNumber(token)) {
                stack.push(parseNumber(token));
            } else if (token.equals(NEGATE)) {
                requireOperands(stack, 1);
                stack.push(-stack.pop());
            } else {
                requireOperands(stack, 2);
                double right = stack.pop(); // popped first because it was pushed last
                double left = stack.pop();
                stack.push(apply(token, left, right));
            }
        }
        if (stack.size() != 1) {
            throw new IllegalArgumentException("the expression is incomplete");
        }
        return stack.pop();
    }

    private double apply(String operator, double left, double right) {
        switch (operator) {
            case "+":
                return left + right;
            case "-":
                return left - right;
            case "*":
                return left * right;
            case "/":
                if (right == 0) {
                    throw new ArithmeticException("you can't divide by zero");
                }
                return left / right;
            case "%":
                if (right == 0) {
                    throw new ArithmeticException("you can't divide by zero");
                }
                return left % right;
            case "^":
                return Math.pow(left, right);
            default:
                throw new IllegalArgumentException("unknown operator " + operator);
        }
    }

    private void requireOperands(Deque<Double> stack, int needed) {
        if (stack.size() < needed) {
            throw new IllegalArgumentException("an operator is missing a number");
        }
    }

    // ---------------- helpers ----------------

    private boolean isNumber(String token) {
        char first = token.charAt(0);
        return Character.isDigit(first) || first == '.';
    }

    private boolean isOperator(String token) {
        return token.equals(NEGATE) || (token.length() == 1 && OPERATORS.contains(token));
    }

    private double parseNumber(String token) {
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + token + "' is not a valid number");
        }
    }

    /** Formats 14.0 as "14" and 0.333333333 as "0.333333". */
    public static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        return BigDecimal.valueOf(value)
                .setScale(6, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
