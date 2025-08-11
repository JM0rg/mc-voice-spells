#!/bin/bash

# Build script for YellSpells native library

set -e

echo "Building YellSpells native library..."

# Create build directory
mkdir -p build/native
cd build/native

# Configure with CMake
cmake ../../src/main/native \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_OSX_ARCHITECTURES="arm64;x86_64"

# Build
cmake --build . --config Release -j$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)

echo "Native library built successfully!"
echo "Library should be in: src/main/resources/natives/"

# List the built libraries
find ../../src/main/resources/natives/ -name "*.dylib" -o -name "*.so" -o -name "*.dll" 2>/dev/null || true
