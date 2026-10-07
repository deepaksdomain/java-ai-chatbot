@echo off
REM Compiles and starts the chatbot (Windows).
REM Usage:  run.bat                  uses data\intents.txt
REM         run.bat my-intents.txt   uses your own knowledge file
cd /d "%~dp0"

if exist out rmdir /s /q out
mkdir out
javac -d out src\main\java\com\nova\chatbot\*.java src\main\java\com\nova\chatbot\core\*.java src\main\java\com\nova\chatbot\knowledge\*.java src\main\java\com\nova\chatbot\matching\*.java src\main\java\com\nova\chatbot\memory\*.java src\main\java\com\nova\chatbot\nlp\*.java src\main\java\com\nova\chatbot\skills\*.java src\main\java\com\nova\chatbot\ui\*.java
if errorlevel 1 exit /b 1

java -cp out com.nova.chatbot.Main %*
