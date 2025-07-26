@echo off
title Collaborative Notepad - CLIENT 1
echo ================================
echo  COLLABORATIVE NOTEPAD CLIENT 1
echo ================================
echo.
echo Current directory: %CD%
echo.
echo Compiling client components...
javac -cp "lib\*" -d . src\util\*.java src\service\*.java src\model\*.java src\*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Client compiled successfully!
    echo.
    echo Starting Client 1...
    echo ================================
    java -cp "lib\*;." src.CollabApp
) else (
    echo.
    echo ❌ Compilation failed!
    echo Check your Java files for errors.
    pause
)