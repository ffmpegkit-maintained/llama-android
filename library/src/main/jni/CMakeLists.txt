cmake_minimum_required(VERSION 3.22.1)
project(llama_android LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# --- 16 KB page alignment (Android 15 ready) -------------------------------
add_link_options(
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384")

# --- llama.cpp submodule at repo root --------------------------------------
# jni dir = library/src/main/jni  →  four levels up is the repo root.
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../llama.cpp)

# Free build: CPU/NEON only. No Vulkan, no OpenMP (avoids libomp.so runtime dep),
# no curl, no CLI tools/examples/server/tests. Shared libs → libllama.so + libggml.so.
set(BUILD_SHARED_LIBS    ON  CACHE BOOL "" FORCE)
set(GGML_OPENMP          OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN          OFF CACHE BOOL "" FORCE)   # Pro flips this ON
set(GGML_LLAMAFILE       OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL           OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON   ON  CACHE BOOL "" FORCE)   # helpers: sampling, chat templates
add_subdirectory(${LLAMA_DIR} ${CMAKE_CURRENT_BINARY_DIR}/llama.cpp)

# --- JNI bridge → libllama_jni.so ------------------------------------------
add_library(llama_jni SHARED llama_jni.cpp)
find_library(log-lib log)

target_include_directories(llama_jni PRIVATE
    ${LLAMA_DIR}/include
    ${LLAMA_DIR}/ggml/include
    ${LLAMA_DIR}/common)

target_link_libraries(llama_jni
    llama
    common
    ${log-lib})
