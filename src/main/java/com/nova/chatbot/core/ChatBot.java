package com.nova.chatbot.core;

import com.nova.chatbot.knowledge.Intent;
import com.nova.chatbot.knowledge.KnowledgeBase;
import com.nova.chatbot.matching.IntentMatcher;
import com.nova.chatbot.matching.KeywordMatcher;
import com.nova.chatbot.matching.MatchResult;
import com.nova.chatbot.matching.TfIdfMatcher;
import com.nova.chatbot.memory.ConversationHistory;
import com.nova.chatbot.memory.Message;
import com.nova.chatbot.memory.Sender;
import com.nova.chatbot.memory.UserProfile;
import com.nova.chatbot.nlp.SpellCorrector;
import com.nova.chatbot.nlp.TextProcessor;
import com.nova.chatbot.skills.MathSkill;
import com.nova.chatbot.skills.MemorySkill;
import com.nova.chatbot.skills.Skill;
import com.nova.chatbot.skills.TimeSkill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * The "brain" of the chatbot. For every message it:
 *
 *   1. saves the message in the conversation history
 *   2. asks each Skill (math, time, memory) if it can answer      -> chain of responsibility
 *   3. otherwise cleans the text and fixes typos                  -> NLP + edit distance
 *   4. ranks intents with the active matching algorithm           -> TF-IDF or keyword
 *   5. uses context for follow-ups like "another one"
 *   6. picks a reply, fills in {name}, and saves it to history
 *
 * Composition: ChatBot HAS-A knowledge base, text processor, spell corrector,
 * matchers, skills, history and user profile. It coordinates them but leaves
 * the details to each one (Single Responsibility Principle).
 */
public class ChatBot {
    public static final String NAME = "Nova";
    public static final String GOODBYE_TAG = "goodbye";
    public static final String FOLLOW_UP_TAG = "follow_up";

    private static final int TOP_K = 3;
    private static final int HISTORY_CAPACITY = 50;
    private static final double SUGGESTION_THRESHOLD = 0.3;
    private static final List<String> FALLBACK_RESPONSES = List.of(
            "I'm not sure I understand. Could you rephrase that?",
            "Hmm, I don't know about that yet. Type /topics to see what I can talk about.",
            "That's a new one for me! Try asking me something else.");

    private final KnowledgeBase knowledgeBase;
    private final TextProcessor textProcessor;
    private final SpellCorrector spellCorrector;
    private final Map<String, IntentMatcher> matchers = new LinkedHashMap<>();
    private final List<Skill> skills;
    private final ConversationHistory history = new ConversationHistory(HISTORY_CAPACITY);
    private final UserProfile profile = new UserProfile();
    private final Map<String, Integer> lastResponseIndex = new HashMap<>();
    private final Random random;

    private IntentMatcher activeMatcher; // not final: can be swapped at runtime (Strategy pattern)
    private Intent lastIntent;           // conversation context for follow-ups

    public ChatBot(KnowledgeBase knowledgeBase, TextProcessor textProcessor,
                   List<IntentMatcher> matcherOptions, List<Skill> skills, Random random) {
        if (matcherOptions == null || matcherOptions.isEmpty()) {
            throw new IllegalArgumentException("At least one IntentMatcher is required");
        }
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase");
        this.textProcessor = Objects.requireNonNull(textProcessor, "textProcessor");
        this.skills = List.copyOf(skills);
        this.random = Objects.requireNonNull(random, "random");
        for (IntentMatcher matcher : matcherOptions) {
            matchers.put(matcher.getName(), matcher);
        }
        this.activeMatcher = matcherOptions.get(0);
        this.spellCorrector = new SpellCorrector(buildVocabulary());
    }

    /** Factory method that wires up the standard setup, so Main stays short. */
    public static ChatBot createDefault(KnowledgeBase knowledgeBase) {
        TextProcessor processor = new TextProcessor();
        List<IntentMatcher> matchers = List.of(
                new TfIdfMatcher(knowledgeBase, processor),
                new KeywordMatcher(knowledgeBase, processor));
        List<Skill> skills = List.of(new MathSkill(), new TimeSkill(), new MemorySkill());
        return new ChatBot(knowledgeBase, processor, matchers, skills, new Random());
    }

    // ------------------------------------------------------------------
    // Main entry point
    // ------------------------------------------------------------------

    public ChatResponse respond(String userInput) {
        // Normalise curly apostrophes (common on phones and Macs) to straight ones.
        String input = userInput == null ? "" : userInput.trim().replace('\u2019', '\'');
        if (input.isEmpty()) {
            return ChatResponse.fallback("Say something and I'll do my best to answer!", List.of(), Map.of());
        }

        history.add(new Message(Sender.USER, input));
        ChatResponse response = trySkills(input).orElseGet(() -> matchIntent(input));
        history.add(new Message(Sender.BOT, response.getText()));
        return response;
    }

    // ------------------------------------------------------------------
    // Step 2: skills (chain of responsibility)
    // ------------------------------------------------------------------

    private Optional<ChatResponse> trySkills(String input) {
        for (Skill skill : skills) {
            // Polymorphism: the same call runs MathSkill, TimeSkill or MemorySkill code.
            Optional<String> answer = skill.tryRespond(input, profile);
            if (answer.isPresent()) {
                return Optional.of(ChatResponse.fromSkill(answer.get(), skill.getName()));
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // Steps 3-6: intent matching
    // ------------------------------------------------------------------

    private ChatResponse matchIntent(String input) {
        // Step 3: clean the text and fix typos.
        Map<String, String> corrections = new LinkedHashMap<>();
        List<String> tokens = new ArrayList<>();
        for (String token : textProcessor.process(input)) {
            String fixed = spellCorrector.correct(token);
            if (!fixed.equals(token)) {
                corrections.put(token, fixed);
            }
            tokens.add(fixed);
        }

        // Step 4: rank intents.
        List<MatchResult> matches = activeMatcher.findTopMatches(tokens, TOP_K);
        boolean confident = !matches.isEmpty()
                && matches.get(0).getScore() >= activeMatcher.getConfidenceThreshold();
        if (!confident) {
            return ChatResponse.fallback(buildFallback(matches), matches, corrections);
        }

        MatchResult best = matches.get(0);
        Intent intent = best.getIntent();

        // Step 5: context. "Another one" continues the previous topic if it has more replies.
        if (intent.getTag().equals(FOLLOW_UP_TAG)) {
            if (lastIntent != null && lastIntent.getResponses().size() > 1) {
                intent = lastIntent;
            }
        } else {
            lastIntent = intent;
        }

        // Step 6: choose and personalise the reply.
        String text = fillPlaceholders(pickResponse(intent));
        return ChatResponse.fromIntent(text, intent.getTag(), best.getScore(), matches, corrections);
    }

    /** Picks a random reply, but never the same one this intent used last time. */
    private String pickResponse(Intent intent) {
        List<String> options = intent.getResponses();
        int index = random.nextInt(options.size());
        Integer previous = lastResponseIndex.get(intent.getTag());
        if (options.size() > 1 && previous != null && index == previous) {
            // Shift by 1..(size-1) places so we land on a different reply.
            index = (index + 1 + random.nextInt(options.size() - 1)) % options.size();
        }
        lastResponseIndex.put(intent.getTag(), index);
        return options.get(index);
    }

    private String fillPlaceholders(String template) {
        return template
                .replace("{name}", profile.getDisplayName())
                .replace("{bot}", NAME);
    }

    private String buildFallback(List<MatchResult> matches) {
        String text = FALLBACK_RESPONSES.get(random.nextInt(FALLBACK_RESPONSES.size()));
        if (!matches.isEmpty() && matches.get(0).getScore() >= SUGGESTION_THRESHOLD) {
            text += " Did you mean something like \"" + matches.get(0).getIntent().getExamplePattern() + "\"?";
        }
        return text;
    }

    /** Every cleaned word that appears in any pattern. Used by the spell corrector. */
    private Set<String> buildVocabulary() {
        Set<String> vocabulary = new HashSet<>();
        for (Intent intent : knowledgeBase.getIntents()) {
            for (String pattern : intent.getPatterns()) {
                vocabulary.addAll(textProcessor.process(pattern));
            }
        }
        return vocabulary;
    }

    // ------------------------------------------------------------------
    // Settings and read-only access for the console
    // ------------------------------------------------------------------

    /** Switches the matching algorithm. Returns false if no matcher has that name. */
    public boolean switchMatcher(String name) {
        IntentMatcher matcher = matchers.get(name);
        if (matcher == null) {
            return false;
        }
        activeMatcher = matcher;
        return true;
    }

    public String getActiveMatcherName() {
        return activeMatcher.getName();
    }

    public Set<String> getMatcherNames() {
        return Collections.unmodifiableSet(matchers.keySet());
    }

    public ConversationHistory getHistory() {
        return history;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public int getVocabularySize() {
        return spellCorrector.vocabularySize();
    }
}
