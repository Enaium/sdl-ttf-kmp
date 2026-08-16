cmake_minimum_required(VERSION 3.16)

project(sdl-ttf-kmp-jni C CXX)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Builds the JNI shared library (libsdl_ttf_jni) for the JVM target.
#
# SDL3 and SDL_ttf (with the vendored FreeType/HarfBuzz/plutosvg) are compiled
# statically and linked into the library, exactly like sdl-kmp's libsdl_jni.
# The process therefore contains a second SDL3 instance; this is safe because
#
#  - SDL_ttf errors are always read through the TTF-side SDL_GetError
#    (SDLTTF.error()), which is the copy the errors were written to;
#  - SDL objects created by the sdl-kmp library (renderers, surfaces,
#    streams) are only operated on through SDL3's function-pointer
#    interfaces, which point into the creating library's copy, so both
#    copies stay consistent.
if(NOT DEFINED SDL_SOURCE_DIR)
    set(SDL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../includes/SDL")
endif()
if(NOT DEFINED SDLTTF_SOURCE_DIR)
    set(SDLTTF_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../includes/SDL_ttf")
endif()

set(SDL_SHARED OFF CACHE BOOL "" FORCE)
set(SDL_STATIC ON CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
set(SDL_EXAMPLES OFF CACHE BOOL "" FORCE)
set(SDL_TESTS OFF CACHE BOOL "" FORCE)
set(SDL_INSTALL OFF CACHE BOOL "" FORCE)
set(SDL_INSTALL_DOCS OFF CACHE BOOL "" FORCE)

add_subdirectory(${SDL_SOURCE_DIR} SDL)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(SDLTTF_VENDORED ON CACHE BOOL "" FORCE)
set(SDLTTF_INSTALL OFF CACHE BOOL "" FORCE)
set(SDLTTF_SAMPLES OFF CACHE BOOL "" FORCE)

add_subdirectory(${SDLTTF_SOURCE_DIR} SDL_ttf)

add_library(sdl_ttf_jni SHARED
    jni_ttf.cpp
)
set_target_properties(sdl_ttf_jni PROPERTIES OUTPUT_NAME sdl_ttf_jni)

# SDL_ttf.h redefines main on Windows (via SDL_main.h); this is a library, not
# an executable, so tell SDL the main() handling is the application's business.
target_compile_definitions(sdl_ttf_jni PRIVATE SDL_MAIN_HANDLED)

target_include_directories(sdl_ttf_jni PRIVATE
    ${SDL_SOURCE_DIR}/include
    ${SDLTTF_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}
)

if(JNI_INCLUDE_DIR)
    target_include_directories(sdl_ttf_jni PRIVATE ${JNI_INCLUDE_DIR})
else()
    find_package(JNI REQUIRED)
    target_include_directories(sdl_ttf_jni PRIVATE ${JNI_INCLUDE_DIRS})
endif()
if(JNI_INCLUDE_DIR_PLATFORM)
    target_include_directories(sdl_ttf_jni PRIVATE ${JNI_INCLUDE_DIR_PLATFORM})
endif()

# SDL3_ttf-static already contains the vendored freetype, harfbuzz and
# plutosvg objects (SDL_ttf's CMake embeds them via TARGET_OBJECTS when
# building static), and SDL3-static propagates the platform system libraries
# (the macOS frameworks, the Windows system libraries, -lm/-ldl/-lpthread on
# Linux) and the harfbuzz backends (CoreText on Apple, usp10/gdi32/rpcrt4 on
# Windows) are propagated by SDL3_ttf-static's link interface.
target_link_libraries(sdl_ttf_jni PRIVATE SDL3_ttf-static SDL3-static)

# SDL3-static is linked into a shared library, so it must be compiled
# position-independent (its default on macOS; required on Linux).
set_target_properties(SDL3-static PROPERTIES POSITION_INDEPENDENT_CODE ON)

if(WIN32)
    set_target_properties(sdl_ttf_jni PROPERTIES PREFIX "")
endif()
