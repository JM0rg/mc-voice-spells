#!/bin/bash

# YellSpells Build Script

echo "Building YellSpells mod..."

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "Gradle wrapper not found. Please run 'gradle wrapper' first."
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Clean and build
./gradlew clean build

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "JAR file location: build/libs/yellspells-1.0.0.jar"
else
    echo "Build failed!"
    exit 1
fi
