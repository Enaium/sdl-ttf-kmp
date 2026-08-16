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

# Builds SDL_ttf (with the vendored FreeType/HarfBuzz/plutosvg) as a static
# library for the Kotlin/Native targets, and copies it to
# SDLTTF_KMP_OUTPUT_DIR/libSDL3_ttf.a where the ttf.def cinterop embeds it
# into the produced klib with -staticLibrary.
#
# SDL3 itself is NOT compiled: SDL_ttf only needs the SDL3 headers, and the
# SDL3 symbols are resolved at the consumer's final link from libSDL3.a, which
# the sdl-kmp klib embeds (the consumer always links it, since the sdl-ttf-kmp
# bindings use the cn.enaium.sdl types).
cmake_minimum_required(VERSION 3.21)
project(sdl-ttf-kmp-native C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

if(NOT DEFINED SDL_SOURCE_DIR)
    set(SDL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../includes/SDL")
endif()
if(NOT DEFINED SDLTTF_SOURCE_DIR)
    set(SDLTTF_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../includes/SDL_ttf")
endif()
if(NOT DEFINED SDLTTF_KMP_OUTPUT_DIR)
    set(SDLTTF_KMP_OUTPUT_DIR "${CMAKE_CURRENT_BINARY_DIR}/out")
endif()

# Fake SDL3 targets so SDL_ttf's CMake configures without building SDL3.
# SDL3::Headers provides the include directory; SDL3::SDL3 is never linked
# (SDL_ttf's static build embeds only its own objects plus the vendored
# freetype/harfbuzz/plutosvg objects via TARGET_OBJECTS).
add_library(SDL3::Headers INTERFACE IMPORTED)
set_target_properties(SDL3::Headers PROPERTIES
    INTERFACE_INCLUDE_DIRECTORIES "${SDL_SOURCE_DIR}/include")
add_library(SDL3::SDL3 INTERFACE IMPORTED)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(SDLTTF_VENDORED ON CACHE BOOL "" FORCE)
set(SDLTTF_INSTALL OFF CACHE BOOL "" FORCE)
set(SDLTTF_SAMPLES OFF CACHE BOOL "" FORCE)

add_subdirectory(${SDLTTF_SOURCE_DIR} SDL_ttf)

# Copy the merged archive (SDL_ttf + freetype + harfbuzz + plutosvg objects)
# into the output directory consumed by the cinterop task.
set(SDLTTF_KMP_MERGED_ARCHIVE "${SDLTTF_KMP_OUTPUT_DIR}/libSDL3_ttf.a")
add_custom_command(
    OUTPUT ${SDLTTF_KMP_MERGED_ARCHIVE}
    COMMAND ${CMAKE_COMMAND} -E make_directory ${SDLTTF_KMP_OUTPUT_DIR}
    COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_FILE:SDL3_ttf-static> ${SDLTTF_KMP_MERGED_ARCHIVE}
    DEPENDS SDL3_ttf-static
    COMMENT "Copying SDL3_ttf static library for Kotlin/Native"
    COMMAND_EXPAND_LISTS
)
add_custom_target(sdl-ttf-kmp-merged ALL DEPENDS ${SDLTTF_KMP_MERGED_ARCHIVE})
