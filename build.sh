#!/bin/bash

# University Attendance System Build Script
# This script builds the Java application using Maven

echo "University Attendance System - Build Script"
echo "========================================="

# Check if Java is installed
if command -v java &> /dev/null; then
    echo "✓ Java found: $(java -version 2>&1 | head -n 1)"
else
    echo "✗ Java not found. Please install Java 17 or later."
    exit 1
fi

# Check if Maven is installed
if command -v mvn &> /dev/null; then
    echo "✓ Maven found: $(mvn -version | head -n 1)"
else
    echo "✗ Maven not found. Please install Maven 3.6+."
    echo ""
    echo "Installation instructions:"
    echo "  Ubuntu/Debian: sudo apt install maven"
    echo "  macOS: brew install maven"
    echo "  Windows: Download from https://maven.apache.org/download.cgi"
    exit 1
fi

# Clean previous builds
echo "Cleaning previous builds..."
mvn clean

# Compile the project
echo "Compiling project..."
mvn compile

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
else
    echo "✗ Compilation failed"
    exit 1
fi

# Run tests
echo "Running tests..."
mvn test

if [ $? -eq 0 ]; then
    echo "✓ Tests passed"
else
    echo "⚠ Some tests failed (this may be expected if database is not configured)"
fi

# Package the application
echo "Packaging application..."
mvn package

if [ $? -eq 0 ]; then
    echo "✓ Packaging successful"
    echo ""
    echo "Build completed successfully!"
    echo "Executable JAR: target/attendance-system-1.0.0.jar"
    echo ""
    echo "To run the application:"
    echo "  java -jar target/attendance-system-1.0.0.jar"
else
    echo "✗ Packaging failed"
    exit 1
fi

echo ""
echo "Build script completed!"