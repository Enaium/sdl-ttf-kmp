/*
 * Copyright (c) 2026 Enaium
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*
 * Compile-time stub for SDL_ttf's GL text engine on iOS/tvOS.
 *
 * SDL3's SDL_opengl.h is empty on iOS/tvOS (no OpenGL there), which leaves
 * SDL_gl_textengine.c without the GL types it needs to compile. This header
 * provides exactly the declarations that file uses, so it can still be
 * compiled (and linked) on those platforms; the engine is dead code there,
 * since nothing creates it. It is forced in with `-include` for that one
 * source file only (see native/CMakeLists.txt).
 */
#ifndef SDL_TTF_KMP_GL_TEXTENGINE_IOS_STUB_H
#define SDL_TTF_KMP_GL_TEXTENGINE_IOS_STUB_H

typedef unsigned int GLenum;
typedef unsigned int GLuint;
typedef int GLint;
typedef int GLsizei;

typedef void (*PFNGLGENTEXTURESPROC)(GLsizei n, GLuint *textures);
typedef void (*PFNGLDELETETEXTURESPROC)(GLsizei n, const GLuint *textures);
typedef void (*PFNGLBINDTEXTUREPROC)(GLenum target, GLuint texture);
typedef void (*PFNGLTEXIMAGE2DPROC)(GLenum target, GLint level, GLint internalformat,
                                    GLsizei width, GLsizei height, GLint border,
                                    GLenum format, GLenum type, const void *pixels);
typedef void (*PFNGLTEXSUBIMAGE2DPROC)(GLenum target, GLint level, GLint xoffset, GLint yoffset,
                                       GLsizei width, GLsizei height,
                                       GLenum format, GLenum type, const void *pixels);
typedef void (*PFNGLPIXELSTOREIPROC)(GLenum pname, GLint param);
typedef void (*PFNGLTEXPARAMETERIPROC)(GLenum target, GLenum pname, GLint param);

#define GL_TEXTURE_2D            0x0DE1
#define GL_TEXTURE_MAG_FILTER    0x2800
#define GL_TEXTURE_MIN_FILTER    0x2801
#define GL_UNPACK_ROW_LENGTH     0x0CF2
#define GL_UNSIGNED_BYTE         0x1401
#define GL_LINEAR                0x2601
#define GL_RGBA                  0x1908
#define GL_BGRA                  0x80E1

#endif /* SDL_TTF_KMP_GL_TEXTENGINE_IOS_STUB_H */
