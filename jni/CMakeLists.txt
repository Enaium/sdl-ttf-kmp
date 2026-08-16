cmake_minimum_required(VERSION 3.16)

project(sdl-ttf-kmp-jni C CXX)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Builds the JNI shared library (libsdl_ttf_jni) for the JVM target.
#
# SDL_ttf (with the vendored FreeType/HarfBuzz/plutosvg) is compiled statically
# into the library, but SDL3 itself is NOT linked: SDL_ttf only needs the SDL3
# headers to compile, and the SDL3 symbols stay undefined so they are resolved
# at dlopen time from libsdl_jni, the JNI library shipped by the sdl-kmp
# project (which statically links the real SDL3). This keeps a single SDL3
# instance per process so global state (errors, hints, the event queue, ...)
# is shared with the SDL3 bindings.
#
# libsdl_jni must therefore be loaded before libsdl_ttf_jni; the Kotlin
# Jni object enforces this (see Jni.kt).
if(NOT DEFINED SDL_SOURCE_DIR)
    set(SDL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../includes/SDL")
endif()
if(NOT DEFINED SDLTTF_SOURCE_DIR)
    set(SDLTTF_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../includes/SDL_ttf")
endif()

# Fake SDL3 targets so SDL_ttf's CMake configures without building SDL3.
# SDL3::Headers provides the include directory; SDL3::SDL3 is never actually
# linked into sdl_ttf_jni (we use the archive file directly below), so an
# INTERFACE target with no location is enough.
add_library(SDL3::Headers INTERFACE IMPORTED)
set_target_properties(SDL3::Headers PROPERTIES
    INTERFACE_INCLUDE_DIRECTORIES "${SDL_SOURCE_DIR}/include")
add_library(SDL3::SDL3 INTERFACE IMPORTED)

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

# Link the SDL3_ttf archive file directly (not the target) so SDL3 does not
# get linked in: its symbols must stay undefined for runtime resolution from
# libsdl_jni. SDL3_ttf-static already contains the vendored freetype,
# harfbuzz and plutosvg objects (SDL_ttf's CMake embeds them via
# TARGET_OBJECTS when building static).
target_link_libraries(sdl_ttf_jni PRIVATE $<TARGET_FILE:SDL3_ttf-static>)

# The archive is referenced by file path above (not as a target), so it has
# no implicit build dependency; make sure it is built.
add_dependencies(sdl_ttf_jni SDL3_ttf-static)

if(APPLE)
    # Resolve the SDL3 symbols at runtime from libsdl_jni (which is loaded
    # first, see Jni.kt) instead of failing the link.
    target_link_options(sdl_ttf_jni PRIVATE -Wl,-undefined,dynamic_lookup)
    # harfbuzz's CoreText backend.
    target_link_libraries(sdl_ttf_jni PRIVATE
        "-framework CoreText"
        "-framework CoreGraphics"
        "-framework CoreFoundation")
elseif(UNIX AND NOT APPLE)
    # GNU ld allows undefined symbols in shared libraries by default; the
    # SDL3 symbols are resolved from libsdl_jni at dlopen time.
    target_link_libraries(sdl_ttf_jni PRIVATE -lm -ldl -lpthread)
elseif(WIN32)
    # harfbuzz's uniscribe/GDI backends. MinGW DLLs resolve undefined
    # externals from already-loaded DLLs (libsdl_jni.dll), which exports the
    # SDL3 symbols.
    target_link_libraries(sdl_ttf_jni PRIVATE -lusp10 -lgdi32 -lrpcrt4)
endif()

if(WIN32)
    set_target_properties(sdl_ttf_jni PROPERTIES PREFIX "")
endif()
