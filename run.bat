@echo off
setlocal enabledelayedexpansion
title Gitaway Hotel Reservation System

echo ============================================================
echo   Gitaway Hotel Reservation System Launcher
echo ============================================================
echo.

rem Check if MySQL JDBC jar exists in lib
if not exist "lib\mysql-connector-j-8.1.0.jar" (
    echo [ERROR] MySQL Connector jar was not found in lib folder!
    echo Please make sure the project is intact.
    pause
    exit /b 1
)

rem Check if Ant is installed and in PATH
where ant >nul 2>&1
if %errorlevel% equ 0 (
    echo [INFO] Apache Ant detected. Building and running with Ant...
    echo.
    call ant run
    goto end
)

rem If Ant is not available, check for Java Development Kit (JDK)
where javac >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] JDK compiler javac was not found in your systems PATH.
    echo Please install Java Development Kit or Apache Ant to run this project.
    echo.
    pause
    exit /b 1
)

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java Runtime Environment was not found in your systems PATH.
    echo.
    pause
    exit /b 1
)

echo [INFO] Ant not found. Compiling manually via JDK...
echo.

if not exist "build\classes" (
    mkdir "build\classes"
)

rem Generate list of java sources as relative paths with forward slashes
if exist sources.txt del sources.txt
for /r src %%i in (*.java) do (
    set "filePath=%%i"
    rem Remove the current directory path (with spaces) from the absolute path
    set "filePath=!filePath:%cd%\=!"
    rem Replace backslashes with forward slashes to avoid javac escape character parsing
    set "filePath=!filePath:\=/!"
    echo !filePath! >> sources.txt
)

echo [INFO] Compiling Java source files...
javac -d build\classes -classpath "lib\mysql-connector-j-8.1.0.jar" @sources.txt
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    if exist sources.txt del sources.txt
    pause
    exit /b 1
)
if exist sources.txt del sources.txt
echo [INFO] Compilation successful!
echo.

echo [INFO] Running the application...
java -classpath "build\classes;lib\mysql-connector-j-8.1.0.jar" com.hotel.Main

:end
echo.
echo Application closed.
pause
