@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
echo Building the NightKeys TEST APK. Android Studio SDK 36 and JDK 17+ are required.
call tools\test-core.cmd
if errorlevel 1 (
    echo Core tests failed. Android build was not started.
    pause
    exit /b 1
)
call gradlew.bat --no-daemon :app:lintDebug :app:assembleDebug
if errorlevel 1 (
    echo.
    echo Build failed. No verified APK has been produced. Keep the error output for debugging.
    pause
    exit /b 1
)
copy /y "app\build\outputs\apk\debug\app-debug.apk" "NightKeys-0.9.5-rc6-test.apk" >nul
echo.
echo Created: %CD%\NightKeys-0.9.5-rc6-test.apk
echo This is a debug-signed test build. Complete the in-app hardware check before daily use.
pause
