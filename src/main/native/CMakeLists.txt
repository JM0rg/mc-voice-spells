cmake_minimum_required(VERSION 3.16)
project(yellspells_whisper)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find JNI
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# Download and build whisper.cpp as a subproject
include(FetchContent)
FetchContent_Declare(
    whisper
    GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp.git
    GIT_TAG        master
)
FetchContent_MakeAvailable(whisper)

# Create our JNI library
add_library(yellspells_whisper SHARED
    cpp/yellspells_whisper.cpp
)

# Link with whisper and JNI
target_link_libraries(yellspells_whisper 
    whisper 
    ${JNI_LIBRARIES}
)

# Include whisper headers
target_include_directories(yellspells_whisper PRIVATE 
    ${whisper_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

# Set output directory based on platform
if(WIN32)
    set_target_properties(yellspells_whisper PROPERTIES 
        OUTPUT_NAME "yellspells_whisper"
        SUFFIX ".dll"
    )
    set(NATIVE_OUTPUT_DIR "${CMAKE_SOURCE_DIR}/../resources/natives/windows")
elseif(APPLE)
    set_target_properties(yellspells_whisper PROPERTIES 
        OUTPUT_NAME "yellspells_whisper"
        SUFFIX ".dylib"
    )
    set(NATIVE_OUTPUT_DIR "${CMAKE_SOURCE_DIR}/../resources/natives/macos")
else()
    set_target_properties(yellspells_whisper PROPERTIES 
        OUTPUT_NAME "yellspells_whisper"
        SUFFIX ".so"
    )
    set(NATIVE_OUTPUT_DIR "${CMAKE_SOURCE_DIR}/../resources/natives/linux")
endif()

# Create output directory and copy library there
add_custom_command(TARGET yellspells_whisper POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E make_directory ${NATIVE_OUTPUT_DIR}
    COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:yellspells_whisper> ${NATIVE_OUTPUT_DIR}/
)

# Compiler-specific optimizations
if(CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    target_compile_options(yellspells_whisper PRIVATE -O3 -ffast-math)
endif()

if(MSVC)
    target_compile_options(yellspells_whisper PRIVATE /O2)
endif()
