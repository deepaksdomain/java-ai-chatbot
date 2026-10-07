package com.nova.chatbot;

import com.nova.chatbot.core.ChatBot;
import com.nova.chatbot.knowledge.KnowledgeBase;
import com.nova.chatbot.knowledge.KnowledgeBaseException;
import com.nova.chatbot.ui.ChatConsole;

import java.nio.file.Path;

/**
 * Program entry point. It only builds the objects and starts the console.
 *
 * Usage:  java -cp out com.nova.chatbot.Main [path/to/intents.txt]
 */
public class Main {
    private static final String DEFAULT_INTENTS_FILE = "data/intents.txt";

    public static void main(String[] args) {
        Path intentsFile = Path.of(args.length > 0 ? args[0] : DEFAULT_INTENTS_FILE);
        try {
            KnowledgeBase knowledgeBase = KnowledgeBase.loadFromFile(intentsFile);
            ChatBot bot = ChatBot.createDefault(knowledgeBase);
            new ChatConsole(bot, System.in, System.out).run();
        } catch (KnowledgeBaseException e) {
            System.err.println("Could not start the chatbot: " + e.getMessage());
            System.err.println("Tip: run the program from the project folder so it can find " + DEFAULT_INTENTS_FILE);
            System.exit(1);
        }
    }
}
