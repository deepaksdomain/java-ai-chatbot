#!/usr/bin/env bash
# Compiles the app plus the tests and runs the test suite (macOS / Linux).
set -e
cd "$(dirname "$0")"

rm -rf out-test
mkdir -p out-test
javac -d out-test \
  src/main/java/com/nova/chatbot/*.java \
  src/main/java/com/nova/chatbot/*/*.java \
  src/test/java/com/nova/chatbot/*.java
java -cp out-test com.nova.chatbot.ChatBotTests
