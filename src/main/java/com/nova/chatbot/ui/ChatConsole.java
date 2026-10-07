package com.nova.chatbot.ui;

import com.nova.chatbot.core.ChatBot;
import com.nova.chatbot.core.ChatResponse;
import com.nova.chatbot.knowledge.Intent;
import com.nova.chatbot.memory.Message;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * The text-based user interface. It reads what the user types, handles
 * slash-commands, and prints the bot's replies.
 *
 * It only talks to ChatBot through public methods and knows nothing about
 * how matching works, so the bot could get a GUI or web front end later
 * without changing any of the bot's code.
 */
public class ChatConsole {
    private static final int HISTORY_TO_SHOW = 10;
    private static final String LINE = "=".repeat(60);

    private final ChatBot bot;
    private final Scanner input;
    private final PrintStream output;
    private boolean debugMode = false;

    public ChatConsole(ChatBot bot, InputStream in, PrintStream out) {
        this.bot = bot;
        this.input = new Scanner(in);
        this.output = out;
    }

    /** The read-respond loop. Runs until the user quits or says goodbye. */
    public void run() {
        printWelcome();
        while (true) {
            output.print("You: ");
            if (!input.hasNextLine()) {
                break; // end of input, e.g. Ctrl+D or the end of a piped file
            }
            String line = input.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("/")) {
                boolean keepGoing = handleCommand(line);
                if (!keepGoing) {
                    break;
                }
                continue;
            }

            ChatResponse response = bot.respond(line);
            output.println(ChatBot.NAME + ": " + response.getText());
            if (debugMode) {
                printDebug(response);
            }
            if (response.isGoodbye()) {
                break;
            }
        }
        output.println(LINE);
        output.println("Chat ended. Thanks for talking with " + ChatBot.NAME + "!");
    }

    /** Returns false when the user wants to quit. */
    private boolean handleCommand(String line) {
        String[] parts = line.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1].trim().toLowerCase() : "";

        switch (command) {
            case "/help" -> printHelp();
            case "/topics" -> printTopics();
            case "/history" -> printHistory();
            case "/mode" -> changeMode(argument);
            case "/debug" -> {
                debugMode = !debugMode;
                output.println("Debug mode is now " + (debugMode ? "ON" : "OFF") + ".");
            }
            case "/quit", "/exit" -> {
                return false;
            }
            default -> output.println("Unknown command \"" + command + "\". Type /help to see all commands.");
        }
        return true;
    }

    private void printWelcome() {
        output.println(LINE);
        output.println("  " + ChatBot.NAME + " - a Java chatbot");
        output.println("  Knows " + bot.getKnowledgeBase().size() + " topics and "
                + bot.getVocabularySize() + " words. Type /help for commands.");
        output.println(LINE);
        output.println(ChatBot.NAME + ": Hi! I'm " + ChatBot.NAME
                + ". Ask me anything, give me a math problem, or tell me your name.");
    }

    private void printHelp() {
        output.println("Commands:");
        output.println("  /help           show this list");
        output.println("  /topics         list everything " + ChatBot.NAME + " knows about");
        output.println("  /history        show the last " + HISTORY_TO_SHOW + " messages");
        output.println("  /mode <name>    switch matching algorithm " + bot.getMatcherNames()
                + " (current: " + bot.getActiveMatcherName() + ")");
        output.println("  /debug          show how the bot picked each answer");
        output.println("  /quit           exit (saying \"bye\" works too)");
        output.println("Things to try: \"tell me a joke\", \"another one\", \"what is 15 * (4 + 2)\",");
        output.println("  \"my name is Alex\", \"what is a stack\", \"what time is it\"");
    }

    private void printTopics() {
        List<String> tags = new ArrayList<>();
        for (Intent intent : bot.getKnowledgeBase().getIntents()) {
            tags.add(intent.getTag());
        }
        output.println("I can talk about: " + String.join(", ", tags));
        output.println("Skills: math, time and date, remembering your name and likes.");
    }

    private void printHistory() {
        List<Message> recent = bot.getHistory().getRecent(HISTORY_TO_SHOW);
        if (recent.isEmpty()) {
            output.println("No messages yet.");
            return;
        }
        output.println("Last " + recent.size() + " messages:");
        for (Message message : recent) {
            output.println("  " + message);
        }
    }

    private void changeMode(String name) {
        if (name.isEmpty()) {
            output.println("Current matcher: " + bot.getActiveMatcherName()
                    + ". Options: " + bot.getMatcherNames());
        } else if (bot.switchMatcher(name)) {
            output.println("Switched to the " + name + " matcher.");
        } else {
            output.println("No matcher called \"" + name + "\". Options: " + bot.getMatcherNames());
        }
    }

    private void printDebug(ChatResponse response) {
        String source = switch (response.getSource()) {
            case SKILL -> "skill '" + response.getHandledBy() + "'";
            case INTENT -> String.format("intent '%s' (confidence %.2f, matcher: %s)",
                    response.getHandledBy(), response.getConfidence(), bot.getActiveMatcherName());
            case FALLBACK -> String.format("fallback (best score %.2f was below the threshold)",
                    response.getConfidence());
        };
        output.println("   [debug] answered by: " + source);
        if (!response.getCorrections().isEmpty()) {
            List<String> fixes = new ArrayList<>();
            for (Map.Entry<String, String> fix : response.getCorrections().entrySet()) {
                fixes.add(fix.getKey() + " -> " + fix.getValue());
            }
            output.println("   [debug] typo fixes: " + String.join(", ", fixes));
        }
        if (!response.getTopMatches().isEmpty()) {
            output.println("   [debug] top matches: " + response.getTopMatches());
        }
    }
}
