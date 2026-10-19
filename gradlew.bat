@echo off
setlocal
set "ROOT=%~dp0"
if defined JAVA_HOME (set "JAVA=%JAVA_HOME%\bin\java.exe") else (set "JAVA=java")
"%JAVA%" "%ROOT%tools\GradleBootstrap.java" "%ROOT%."
if errorlevel 1 exit /b 1
call "%ROOT%.local-tools\gradle-8.13\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
