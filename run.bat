@echo off
echo ===============================================
echo University Attendance System
echo ===============================================
echo.
echo Checking Java installation...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found!
    echo Please install Java 17+ from: https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
)
echo Java found successfully.
echo.

echo Checking Maven installation...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo WARNING: Maven not found in PATH
    echo You can still run the application, but cannot build from source.
    echo.
    echo If JAR file exists, trying to run it directly...
    if exist "target\attendance-system-1.0.0.jar" (
        echo Found JAR file, starting application...
        java -jar target\attendance-system-1.0.0.jar
    ) else (
        echo ERROR: JAR file not found and Maven not available.
        echo Please install Maven from: https://maven.apache.org/download.cgi
    )
    pause
    exit /b 1
)
echo Maven found successfully.
echo.

echo Building application...
mvn clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Build failed!
    echo Please check the error messages above.
    pause
    exit /b 1
)
echo.
echo Build successful!
echo.
echo Starting University Attendance System...
echo ===============================================
java -jar target\attendance-system-1.0.0.jar
echo.
echo Application closed.
pause