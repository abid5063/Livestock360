@echo off
echo 🚀 Building Livestock360 Simple Java Backend...

echo 📦 Installing dependencies with Maven...
call mvn clean compile

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Maven build failed
    pause
    exit /b 1
)

echo ✅ Build successful!
echo 🚀 Starting server on port 5000...
call mvn exec:java

pause
