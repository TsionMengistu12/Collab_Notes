@echo off
title Collaborative Notepad - SERVER
echo ================================
echo  COLLABORATIVE NOTEPAD SERVER
echo ================================
echo.
echo Current directory: %CD%
echo.
echo Compiling server components...

REM Compile all Java files with correct classpath
javac -cp "lib\*" -d . src\util\*.java src\service\*.java src\model\*.java src\CollabServer.java src\ClientHandler.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Server compiled successfully!
    echo.
    echo Starting server on localhost:5000...
    echo ================================
    REM Run with correct classpath
    java -cp "lib\*;." src.CollabServer
) else (
    echo.
    echo ❌ Compilation failed!
    echo Check your Java files for errors.
    pause
)