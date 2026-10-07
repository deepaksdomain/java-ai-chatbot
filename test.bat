@echo off
REM Compiles the app plus the tests and runs the test suite (Windows).
cd /d "%~dp0"

if exist out-test rmdir /s /q out-test
mkdir out-test
javac -d out-test src\main\java\com\nova\chatbot\*.java src\main\java\com\nova\chatbot\core\*.java src\main\java\com\nova\chatbot\knowledge\*.java src\main\java\com\nova\chatbot\matching\*.java src\main\java\com\nova\chatbot\memory\*.java src\main\java\com\nova\chatbot\nlp\*.java src\main\java\com\nova\chatbot\skills\*.java src\main\java\com\nova\chatbot\ui\*.java src\test\java\com\nova\chatbot\*.java
if errorlevel 1 exit /b 1

java -cp out-test com.nova.chatbot.ChatBotTests
