#!/usr/bin/env bash
# Compiles and starts the chatbot (macOS / Linux).
# Usage:  ./run.sh                     uses data/intents.txt
#         ./run.sh my-intents.txt      uses your own knowledge file
set -e
cd "$(dirname "$0")"

rm -rf out
mkdir -p out
javac -d out src/main/java/com/nova/chatbot/*.java src/main/java/com/nova/chatbot/*/*.java
java -cp out com.nova.chatbot.Main "$@"
