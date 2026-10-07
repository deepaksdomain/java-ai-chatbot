package com.nova.chatbot;

import com.nova.chatbot.core.ChatBot;
import com.nova.chatbot.core.ChatResponse;
import com.nova.chatbot.knowledge.Intent;
import com.nova.chatbot.knowledge.KnowledgeBase;
import com.nova.chatbot.knowledge.KnowledgeBaseException;
import com.nova.chatbot.matching.IntentMatcher;
import com.nova.chatbot.matching.KeywordMatcher;
import com.nova.chatbot.matching.MatchResult;
import com.nova.chatbot.matching.Ranker;
import com.nova.chatbot.matching.TfIdfMatcher;
import com.nova.chatbot.memory.ConversationHistory;
import com.nova.chatbot.memory.Message;
import com.nova.chatbot.memory.Sender;
import com.nova.chatbot.memory.UserProfile;
import com.nova.chatbot.nlp.SpellCorrector;
import com.nova.chatbot.nlp.TextProcessor;
import com.nova.chatbot.skills.ExpressionEvaluator;
import com.nova.chatbot.skills.MathSkill;
import com.nova.chatbot.skills.MemorySkill;
import com.nova.chatbot.skills.Skill;
import com.nova.chatbot.skills.TimeSkill;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * A small, dependency-free test suite. Each check prints PASS or FAIL, and the
 * program exits with code 1 if anything failed (so it can run in CI).
 *
 * Run from the project folder:  ./test.sh   (or test.bat on Windows)
 */
public class ChatBotTests {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        KnowledgeBase kb = KnowledgeBase.loadFromFile(Path.of("data/intents.txt"));

        testExpressionEvaluator();
        testEditDistance();
        testSpellCorrector();
        testTextProcessor();
        testConversationHistory();
        testRanker();
        testKnowledgeBaseParsing();
        testMatchers(kb);
        testSkills();
        testChatBotFlow(kb);

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------

    private static void testExpressionEvaluator() {
        section("ExpressionEvaluator (stacks + Shunting-Yard)");
        ExpressionEvaluator calc = new ExpressionEvaluator();
        checkEquals("precedence: 3 + 4 * 2", 11.0, calc.evaluate("3 + 4 * 2"));
        checkEquals("parentheses: (3 + 4) * 2", 14.0, calc.evaluate("(3 + 4) * 2"));
        checkEquals("right-associative power: 2 ^ 3 ^ 2", 512.0, calc.evaluate("2 ^ 3 ^ 2"));
        checkEquals("unary minus binds after power: -2 ^ 2", -4.0, calc.evaluate("-2 ^ 2"));
        checkEquals("negative operand: 2 * -3", -6.0, calc.evaluate("2 * -3"));
        checkEquals("decimals: 10 / 4", 2.5, calc.evaluate("10 / 4"));
        checkEquals("modulo: 10 % 3", 1.0, calc.evaluate("10 % 3"));
        checkEquals("nested: ((1 + 2) * (3 + 4)) / 7", 3.0, calc.evaluate("((1 + 2) * (3 + 4)) / 7"));
        checkEquals("format whole number", "14", ExpressionEvaluator.format(14.0));
        checkEquals("format repeating decimal", "0.333333", ExpressionEvaluator.format(1.0 / 3));
        checkThrows("division by zero", ArithmeticException.class, () -> calc.evaluate("5 / 0"));
        checkThrows("missing parenthesis", IllegalArgumentException.class, () -> calc.evaluate("(1 + 2"));
        checkThrows("dangling operator", IllegalArgumentException.class, () -> calc.evaluate("3 +"));
        checkThrows("letters are rejected", IllegalArgumentException.class, () -> calc.evaluate("3 + x"));
    }

    private static void testEditDistance() {
        section("Edit distance (dynamic programming)");
        checkEquals("kitten -> sitting", 3, SpellCorrector.editDistance("kitten", "sitting"));
        checkEquals("empty -> abc", 3, SpellCorrector.editDistance("", "abc"));
        checkEquals("identical words", 0, SpellCorrector.editDistance("java", "java"));
        checkEquals("swapped letters count as 1", 1, SpellCorrector.editDistance("jkoe", "joke"));
    }

    private static void testSpellCorrector() {
        section("SpellCorrector");
        SpellCorrector corrector = new SpellCorrector(List.of("joke", "weather", "hello", "algorithm"));
        checkEquals("extra letter", "joke", corrector.correct("jokke"));
        checkEquals("missing letter", "weather", corrector.correct("wether"));
        checkEquals("two typos in a long word", "algorithm", corrector.correct("algorytm"));
        checkEquals("known word unchanged", "hello", corrector.correct("hello"));
        checkEquals("nothing close stays the same", "pizza", corrector.correct("pizza"));
        checkEquals("short words are not corrected", "hi", corrector.correct("hi"));
    }

    private static void testTextProcessor() {
        section("TextProcessor");
        TextProcessor processor = new TextProcessor();
        checkEquals("lowercase, punctuation, stop words, stemming",
                List.of("what", "weather", "like"), processor.process("What's the WEATHER like?"));
        checkEquals("plural is stemmed", List.of("tell", "some", "joke"), processor.process("Tell me some jokes!"));
        checkEquals("tokenize keeps stop words", List.of("i", "am", "here"), processor.tokenize("I am here."));
        checkEquals("empty input", List.of(), processor.process("  ?!  "));
        checkEquals("contractions are expanded", List.of("you", "awesome"), processor.process("You're awesome"));
        checkEquals("negative contractions keep 'not'", List.of("not", "sure"), processor.process("I'm not sure"));
        checkEquals("curly apostrophes work too", List.of("what", "up"), processor.process("What\u2019s up"));
    }

    private static void testConversationHistory() {
        section("ConversationHistory (bounded queue)");
        ConversationHistory history = new ConversationHistory(3);
        for (int i = 1; i <= 5; i++) {
            history.add(new Message(Sender.USER, "message " + i));
        }
        checkEquals("never grows past capacity", 3, history.size());
        List<Message> recent = history.getRecent(10);
        checkEquals("oldest messages were evicted first", "message 3", recent.get(0).getText());
        checkEquals("newest message is last", "message 5", recent.get(2).getText());
        checkEquals("getRecent(2) returns the last two", "message 4", history.getRecent(2).get(0).getText());
        checkThrows("capacity must be positive", IllegalArgumentException.class, () -> new ConversationHistory(0));
    }

    private static void testRanker() {
        section("Ranker (min-heap top-k)");
        Map<Intent, Double> scores = new LinkedHashMap<>();
        scores.put(intent("a"), 0.10);
        scores.put(intent("b"), 0.90);
        scores.put(intent("c"), 0.50);
        scores.put(intent("d"), 0.70);
        scores.put(intent("e"), 0.00);
        List<MatchResult> top = Ranker.topK(scores, 2);
        checkEquals("keeps only k results", 2, top.size());
        checkEquals("best is first", "b", top.get(0).getIntent().getTag());
        checkEquals("second best is second", "d", top.get(1).getIntent().getTag());
        checkEquals("zero scores are skipped", 4, Ranker.topK(scores, 10).size());
    }

    private static void testKnowledgeBaseParsing() {
        section("KnowledgeBase parsing");
        try {
            KnowledgeBase kb = KnowledgeBase.parse(List.of(
                    "# comment", "", "[greeting]", "patterns: hi | hello", "responses: Hey!",
                    "[bye]", "patterns: bye", "responses: See ya | Later"));
            checkEquals("reads two intents", 2, kb.size());
            checkEquals("splits patterns on |", 2, kb.findByTag("greeting").orElseThrow().getPatterns().size());
            check("lookup of unknown tag is empty", kb.findByTag("nope").isEmpty());
        } catch (KnowledgeBaseException e) {
            fail("valid file should parse: " + e.getMessage());
        }
        checkThrowsChecked("intent without responses",
                () -> KnowledgeBase.parse(List.of("[x]", "patterns: hi")));
        checkThrowsChecked("patterns before any tag",
                () -> KnowledgeBase.parse(List.of("patterns: hi")));
        checkThrowsChecked("duplicate tag",
                () -> KnowledgeBase.parse(List.of("[x]", "patterns: a", "responses: b",
                        "[x]", "patterns: c", "responses: d")));
        checkThrowsChecked("unknown line",
                () -> KnowledgeBase.parse(List.of("[x]", "pattern: oops")));
    }

    private static void testMatchers(KnowledgeBase kb) {
        section("Intent matchers (TF-IDF and keyword)");
        TextProcessor processor = new TextProcessor();
        List<IntentMatcher> matchers = List.of(new TfIdfMatcher(kb, processor), new KeywordMatcher(kb, processor));
        for (IntentMatcher matcher : matchers) {
            checkEquals(matcher.getName() + ": greeting", "greeting", bestTag(matcher, processor, "hi there!"));
            checkEquals(matcher.getName() + ": joke", "joke", bestTag(matcher, processor, "tell me a joke please"));
            checkEquals(matcher.getName() + ": stack", "stack", bestTag(matcher, processor, "what is a stack"));
            checkEquals(matcher.getName() + ": identity", "bot_identity", bestTag(matcher, processor, "what's your name?"));
            checkEquals(matcher.getName() + ": capabilities", "capabilities", bestTag(matcher, processor, "what can you do?"));
            checkEquals(matcher.getName() + ": compliment", "compliment", bestTag(matcher, processor, "you are awesome"));
            check(matcher.getName() + ": results sorted best first", isSortedDescending(
                    matcher.findTopMatches(processor.process("what is a hash map"), 3)));
        }
    }

    private static void testSkills() {
        section("Skills");
        UserProfile profile = new UserProfile();

        Skill math = new MathSkill();
        checkEquals("math with words", "12 * (3 + 2) = 60",
                math.tryRespond("What is 12 * (3 + 2)?", profile).orElse("none"));
        check("math ignores plain text", math.tryRespond("what is a stack", profile).isEmpty());
        check("math reports errors politely",
                math.tryRespond("calculate 1/0", profile).orElse("").contains("divide by zero"));

        Clock fixed = Clock.fixed(Instant.parse("2026-10-06T21:49:00Z"), ZoneId.of("America/New_York"));
        Skill time = new TimeSkill(fixed);
        checkEquals("time", "It's 5:49 PM.", time.tryRespond("what time is it?", profile).orElse("none"));
        checkEquals("date", "Today is Tuesday, October 6, 2026.",
                time.tryRespond("what's the date", profile).orElse("none"));

        Skill memory = new MemorySkill();
        checkEquals("forgets nothing before being told",
                "You haven't told me your name yet. Try saying \"my name is ...\"",
                memory.tryRespond("what's my name", profile).orElse("none"));
        memory.tryRespond("Hi, my name is alex", profile);
        checkEquals("stores a capitalised name", "Alex", profile.getName().orElse("none"));
        checkEquals("recalls the name", "Your name is Alex.", memory.tryRespond("what is my name?", profile).orElse("none"));
        memory.tryRespond("I like pizza", profile);
        memory.tryRespond("I really love hiking!", profile);
        memory.tryRespond("I like pizza", profile); // duplicate is ignored by the set
        checkEquals("recalls likes in order", "You told me you like pizza and hiking.",
                memory.tryRespond("what do I like", profile).orElse("none"));
        check("ignores 'I love you'", memory.tryRespond("I love you", profile).isEmpty());
    }

    private static void testChatBotFlow(KnowledgeBase kb) {
        section("ChatBot end-to-end");
        TextProcessor processor = new TextProcessor();
        ChatBot bot = new ChatBot(kb, processor,
                List.of(new TfIdfMatcher(kb, processor), new KeywordMatcher(kb, processor)),
                List.of(new MathSkill(), new TimeSkill(), new MemorySkill()),
                new Random(42)); // fixed seed = repeatable results

        ChatResponse typo = bot.respond("tell me a jokke");
        checkEquals("typo still finds the joke intent", "joke", typo.getHandledBy());
        checkEquals("typo correction is reported", "joke", typo.getCorrections().get("jokke"));

        ChatResponse another = bot.respond("another one");
        checkEquals("follow-up continues the last topic", "joke", another.getHandledBy());
        check("follow-up gives a different joke", !another.getText().equals(typo.getText()));

        checkEquals("skills run before intents", ChatResponse.Source.SKILL, bot.respond("2 + 2").getSource());

        bot.respond("my name is Sam");
        check("greeting uses the remembered name", bot.respond("hello").getText().contains("Sam"));

        ChatResponse unknown = bot.respond("quantum chromodynamics lagrangian");
        checkEquals("unknown topics fall back", ChatResponse.Source.FALLBACK, unknown.getSource());

        check("bye ends the chat", bot.respond("bye").isGoodbye());
        check("history records both sides", bot.getHistory().size() == 14);

        check("can switch to keyword matcher", bot.switchMatcher("keyword"));
        checkEquals("active matcher changed", "keyword", bot.getActiveMatcherName());
        check("unknown matcher is rejected", !bot.switchMatcher("magic"));
        checkEquals("keyword matcher still answers", "joke", bot.respond("make me laugh").getHandledBy());
    }

    // ------------------------------------------------------------------
    // Tiny assertion helpers
    // ------------------------------------------------------------------

    private static void section(String name) {
        System.out.println();
        System.out.println("== " + name + " ==");
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            fail(name);
        }
    }

    private static void checkEquals(String name, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            check(name, true);
        } else {
            fail(name + "  (expected: " + expected + ", got: " + actual + ")");
        }
    }

    private static void checkThrows(String name, Class<? extends Throwable> type, Runnable code) {
        try {
            code.run();
            fail(name + "  (expected " + type.getSimpleName() + ", nothing was thrown)");
        } catch (Throwable t) {
            if (type.isInstance(t)) {
                check(name, true);
            } else {
                fail(name + "  (expected " + type.getSimpleName() + ", got " + t.getClass().getSimpleName() + ")");
            }
        }
    }

    /** For code that throws the checked KnowledgeBaseException. */
    private interface CheckedAction {
        void run() throws KnowledgeBaseException;
    }

    private static void checkThrowsChecked(String name, CheckedAction code) {
        try {
            code.run();
            fail(name + "  (expected KnowledgeBaseException, nothing was thrown)");
        } catch (KnowledgeBaseException e) {
            check(name, true);
        }
    }

    private static void fail(String name) {
        failed++;
        System.out.println("  FAIL  " + name);
    }

    private static Intent intent(String tag) {
        return new Intent(tag, List.of("pattern"), List.of("response"));
    }

    private static String bestTag(IntentMatcher matcher, TextProcessor processor, String text) {
        List<MatchResult> results = matcher.findTopMatches(processor.process(text), 1);
        return results.isEmpty() ? "none" : results.get(0).getIntent().getTag();
    }

    private static boolean isSortedDescending(List<MatchResult> results) {
        for (int i = 1; i < results.size(); i++) {
            if (results.get(i - 1).getScore() < results.get(i).getScore()) {
                return false;
            }
        }
        return !results.isEmpty();
    }
}
