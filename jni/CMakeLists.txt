cmake_minimum_required(VERSION 3.16)

project(sdl-ttf-kmp-jni C CXX)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Builds the JNI shared library (libsdl_ttf_jni) for the JVM target.
#
# SDL3 is NOT linked: a single SDL3 instance per process is required, because
# SDL3 validates its objects (SDL_Texture, SDL_Surface, ...) with magic
# numbers against its own object registry - a second copy would reject every
# object created by the first. SDL_ttf (with the vendored FreeType/HarfBuzz/
# plutosvg) is compiled statically, and its SDL3 symbols are resolved at
# runtime from libsdl_jni, the JNI library of the sdl-kmp JVM artifact:
#
#  - macOS/Linux: the symbols stay undefined in libsdl_ttf_jni and are
#    resolved at dlopen time from libsdl_jni, which exports them (SDL3 is
#    compiled with default visibility there). libsdl_jni must therefore be
#    loaded first; the Kotlin Jni object enforces this.
#  - Windows: MinGW's linker refuses undefined symbols in DLLs, so the
#    build links the actual libsdl_jni.dll (extracted from the sdl-kmp JVM
#    artifact by the Gradle build and passed as SDL_JNI_DLL): its exports
#    become import entries and resolve at load time. When SDL_JNI_DLL is not
#    provided (local developer builds without the sdl-kmp jar), SDL3 is
#    compiled and linked in as a fallback (two copies: works for standalone
#    use, but cross-copy SDL3 objects are rejected).
if(NOT DEFINED SDL_SOURCE_DIR)
    set(SDL_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../includes/SDL")
endif()
if(NOT DEFINED SDLTTF_SOURCE_DIR)
    set(SDLTTF_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../includes/SDL_ttf")
endif()

if(WIN32 AND DEFINED SDL_JNI_DLL AND EXISTS "${SDL_JNI_DLL}")
    # SDL3::Headers provides the include directory; SDL3::SDL3 is only used
    # by SDL_ttf's CMake for its find_package check. SDL3 itself is not
    # compiled; its symbols are resolved from libsdl_jni.dll.
    add_library(SDL3::Headers INTERFACE IMPORTED)
    set_target_properties(SDL3::Headers PROPERTIES
        INTERFACE_INCLUDE_DIRECTORIES "${SDL_SOURCE_DIR}/include")
    add_library(SDL3::SDL3 INTERFACE IMPORTED)
elseif(WIN32)
    # Fallback (no SDL_JNI_DLL provided): compile SDL3 itself. SDL's own CMake
    # creates the SDL3::Headers / SDL3::SDL3 alias targets, so the fake
    # targets must NOT be created here (an existing target would make SDL's
    # add_library(ALIAS) fail). SDL_ttf is added after SDL so its
    # `if(NOT TARGET SDL3::Headers OR NOT TARGET SDL3::SDL3)` check skips the
    # find_package call.
    set(SDL_SHARED OFF CACHE BOOL "" FORCE)
    set(SDL_STATIC ON CACHE BOOL "" FORCE)
    set(SDL_TEST OFF CACHE BOOL "" FORCE)
    set(SDL_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(SDL_TESTS OFF CACHE BOOL "" FORCE)
    set(SDL_INSTALL OFF CACHE BOOL "" FORCE)
    add_subdirectory(${SDL_SOURCE_DIR} SDL)
else()
    # Fake SDL3 targets so SDL_ttf's CMake configures without building SDL3.
    add_library(SDL3::Headers INTERFACE IMPORTED)
    set_target_properties(SDL3::Headers PROPERTIES
        INTERFACE_INCLUDE_DIRECTORIES "${SDL_SOURCE_DIR}/include")
    add_library(SDL3::SDL3 INTERFACE IMPORTED)
endif()

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
# plutosvg objects, and its link interface propagates the harfbuzz backends
# (CoreText on Apple, usp10/gdi32/rpcrt4 on Windows). The archive is
# referenced by file path (not as a target) so SDL3 stays out of the link.
target_link_libraries(sdl_ttf_jni PRIVATE $<TARGET_FILE:SDL3_ttf-static>)
add_dependencies(sdl_ttf_jni SDL3_ttf-static)

if(WIN32 AND DEFINED SDL_JNI_DLL AND EXISTS "${SDL_JNI_DLL}")
    # Linking the DLL file itself makes ld build import entries for its
    # exports (the SDL3 symbols), resolving the undefined references.
    target_link_libraries(sdl_ttf_jni PRIVATE "${SDL_JNI_DLL}")
elseif(APPLE)
    # Resolve the SDL3 symbols at runtime from libsdl_jni (loaded first, see
    # Jni.kt) instead of failing the link.
    target_link_options(sdl_ttf_jni PRIVATE -Wl,-undefined,dynamic_lookup)
    # harfbuzz's CoreText backend.
    target_link_libraries(sdl_ttf_jni PRIVATE
        "-framework CoreText"
        "-framework CoreGraphics"
        "-framework CoreFoundation")
elseif(UNIX AND NOT APPLE)
    if(DEFINED SDL_JNI_DLL AND EXISTS "${SDL_JNI_DLL}")
        # Linking the actual libsdl_jni.so (extracted from the sdl-kmp JVM
        # artifact by the Gradle build) creates a DT_NEEDED entry, so the
        # loader resolves the SDL3 symbols regardless of how the JVM loaded
        # libsdl_jni (RTLD_LOCAL hides its symbols from global lookups).
        target_link_libraries(sdl_ttf_jni PRIVATE "${SDL_JNI_DLL}")
    else()
        # GNU ld allows undefined symbols in shared libraries by default; the
        # SDL3 symbols are resolved from libsdl_jni at dlopen time.
        target_link_libraries(sdl_ttf_jni PRIVATE -lm -ldl -lpthread)
    endif()
elseif(WIN32)
    # Fallback (no SDL_JNI_DLL provided): SDL3 was compiled in the
    # configuration step above; link it. SDL's dynapi stub also defines a
    # JNI_OnLoad; the first definition (ours, in jni_ttf.cpp) wins with
    # --allow-multiple-definition.
    target_link_libraries(sdl_ttf_jni PRIVATE SDL3-static)
    target_link_options(sdl_ttf_jni PRIVATE -Wl,--allow-multiple-definition)
endif()

if(WIN32)
    set_target_properties(sdl_ttf_jni PROPERTIES PREFIX "")
endif()
