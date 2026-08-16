# Included by CMake at the top of SDL_ttf's project() call (see
# CMAKE_PROJECT_INCLUDE in CMakeLists.txt), i.e. in the SDL_ttf CMake
# directory scope, before its targets are defined.
#
# SDL3's SDL_opengl.h is empty on iOS/tvOS (no OpenGL there), so SDL_ttf's GL
# text engine (SDL_gl_textengine.c) has no GL types to compile against on
# those platforms. CMake cannot drop a source from a target defined by
# another project, so the file is compiled with a forced include of a stub
# header that provides exactly the declarations it uses; the engine is dead
# code on iOS/tvOS (nothing creates it).
if(CMAKE_SYSTEM_NAME STREQUAL "iOS" OR CMAKE_SYSTEM_NAME STREQUAL "tvOS")
    set_source_files_properties(
        ${CMAKE_CURRENT_SOURCE_DIR}/src/SDL_gl_textengine.c
        PROPERTIES COMPILE_OPTIONS "-include;${SDL_TTF_KMP_STUB_DIR}/gl_textengine_ios_stub.h"
    )
endif()
