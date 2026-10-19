@echo off
setlocal
cd /d "%~dp0.."
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
if not exist "build\core-tests" mkdir "build\core-tests"
javac --release 17 -d "build\core-tests" app\src\main\java\com\everyonesniche\nightkeys\core\*.java core-tests\CoreTests.java
if errorlevel 1 exit /b 1
java -cp "build\core-tests" CoreTests
exit /b %ERRORLEVEL%
