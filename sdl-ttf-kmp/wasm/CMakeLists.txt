# Copyright (c) 2026 Enaium
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

# Builds SDL3 and SDL_ttf (with the vendored FreeType/HarfBuzz/plutosvg) as
# static libraries for the Emscripten (wasmJs) target. The Gradle linkWasmTtf
# task then compiles the sdl-kmp SDL3 shim plus the TTF shim against them and
# links everything into a single Emscripten module (sdl_wasm.js).
#
# SDL3 and SDL_ttf MUST live in the same module: TTF_Surface handles produced
# by SDL_ttf are consumed by the sdl-kmp renderer/surface bindings, so the two
# libraries have to share one Emscripten heap.
cmake_minimum_required(VERSION 3.21)
project(sdl-ttf-kmp-wasm C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

if(NOT DEFINED SDL_SOURCE_DIR)
    set(SDL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../includes/SDL")
endif()
if(NOT DEFINED SDLTTF_SOURCE_DIR)
    set(SDLTTF_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../includes/SDL_ttf")
endif()
if(NOT DEFINED WASM_OUTPUT_DIR)
    set(WASM_OUTPUT_DIR "${CMAKE_CURRENT_BINARY_DIR}/out")
endif()

# SDL3
set(SDL_SHARED OFF CACHE BOOL "" FORCE)
set(SDL_STATIC ON CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
set(SDL_EXAMPLES OFF CACHE BOOL "" FORCE)
set(SDL_TESTS OFF CACHE BOOL "" FORCE)
set(SDL_INSTALL OFF CACHE BOOL "" FORCE)
set(SDL_INSTALL_DOCS OFF CACHE BOOL "" FORCE)

add_subdirectory(${SDL_SOURCE_DIR} SDL)

# SDL_ttf. Building SDL3 first means SDL_ttf's find_package(SDL3) is skipped:
# the SDL3::Headers and SDL3::SDL3 targets already exist.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(SDLTTF_VENDORED ON CACHE BOOL "" FORCE)
set(SDLTTF_INSTALL OFF CACHE BOOL "" FORCE)
set(SDLTTF_SAMPLES OFF CACHE BOOL "" FORCE)

# The vendored HarfBuzz promotes -Wunused to an error with
# #pragma GCC diagnostic. Emscripten's clang 24 expanded -Wunused to include
# -Wunused-template, so every unused function template in HarfBuzz's headers
# becomes a hard error. Disable HarfBuzz's error pragmas for this build; the
# upstream build has not been updated for that compiler yet.
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHB_NO_PRAGMA_GCC_DIAGNOSTIC_ERROR")

add_subdirectory(${SDLTTF_SOURCE_DIR} SDL_ttf)

# Emit copies of both archives into the output directory consumed by the
# Gradle link task.
add_custom_command(
    OUTPUT ${WASM_OUTPUT_DIR}/libSDL3.a
    COMMAND ${CMAKE_COMMAND} -E make_directory ${WASM_OUTPUT_DIR}
    COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_FILE:SDL3-static> ${WASM_OUTPUT_DIR}/libSDL3.a
    DEPENDS SDL3-static
    COMMENT "Copying SDL3 static library for wasm"
)
add_custom_target(sdl3-kmp-wasm-merged ALL DEPENDS ${WASM_OUTPUT_DIR}/libSDL3.a)

add_custom_command(
    OUTPUT ${WASM_OUTPUT_DIR}/libSDL3_ttf.a
    COMMAND ${CMAKE_COMMAND} -E make_directory ${WASM_OUTPUT_DIR}
    COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_FILE:SDL3_ttf-static> ${WASM_OUTPUT_DIR}/libSDL3_ttf.a
    DEPENDS SDL3_ttf-static
    COMMENT "Copying SDL3_ttf static library for wasm"
)
add_custom_target(sdl-ttf-kmp-wasm-merged ALL DEPENDS ${WASM_OUTPUT_DIR}/libSDL3_ttf.a)
