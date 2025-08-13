@echo off
echo 🔨 Compiling and running standalone Java backend...

REM Create lib directory if it doesn't exist
if not exist "lib" mkdir lib

REM Download dependencies if they don't exist
if not exist "lib\mongodb-driver-sync-4.11.1.jar" (
    echo 📦 Downloading MongoDB driver...
    curl -L -o "lib\mongodb-driver-sync-4.11.1.jar" "https://repo1.maven.org/maven2/org/mongodb/mongodb-driver-sync/4.11.1/mongodb-driver-sync-4.11.1.jar"
)

if not exist "lib\mongodb-driver-core-4.11.1.jar" (
    echo 📦 Downloading MongoDB driver core...
    curl -L -o "lib\mongodb-driver-core-4.11.1.jar" "https://repo1.maven.org/maven2/org/mongodb/mongodb-driver-core/4.11.1/mongodb-driver-core-4.11.1.jar"
)

if not exist "lib\bson-4.11.1.jar" (
    echo 📦 Downloading BSON library...
    curl -L -o "lib\bson-4.11.1.jar" "https://repo1.maven.org/maven2/org/mongodb/bson/4.11.1/bson-4.11.1.jar"
)

if not exist "lib\gson-2.10.1.jar" (
    echo 📦 Downloading Gson library...
    curl -L -o "lib\gson-2.10.1.jar" "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
)

echo 🔨 Compiling Java backend...
javac -cp "lib\*" LivestockBackend.java

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Compilation failed
    pause
    exit /b 1
)

echo ✅ Compilation successful!
echo 🚀 Starting Livestock360 Java Backend on port 5000...
java -cp ".;lib\*" LivestockBackend

pause
