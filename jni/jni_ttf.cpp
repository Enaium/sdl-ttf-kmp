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
 * JNI bridge for the sdl-ttf-kmp JVM target.
 *
 * Every `external fun` on the Kotlin `cn.enaium.sdl.ttf.Jni` object maps 1:1
 * to a `Java_cn_enaium_sdl_ttf_Jni_<name>` function in this file (see the
 * naming convention in sdl-kmp's jni_bridge.h).
 *
 * The library statically links its own SDL3 and SDL_ttf (like sdl-kmp's
 * libsdl_jni), so TTF_Font, TTF_TextEngine, TTF_Text and the SDL_* handles
 * are passed across as opaque 64-bit pointers and the copies of SDL3 in the
 * process do not interfere (see jni/CMakeLists.txt).
 *
 * Error convention: on failure the TTF functions set SDL's error string
 * (SDL_GetError). [Java_cn_enaium_sdl_ttf_Jni_getError] reads it from the
 * TTF-side copy of SDL3, which is the copy the error was written to.
 */

#include <jni.h>
#include <stdint.h>
#include <string>
#include <vector>

#include <SDL3/SDL.h>
#include <SDL3_ttf/SDL_ttf.h>

// JNI entry-point naming macro: every external fun on the Kotlin
// `cn.enaium.sdl.ttf.Jni` object maps to Java_cn.enaium.sdl.ttf_Jni_<name>.
#define TTFJNI_FUNC(ret) extern "C" JNIEXPORT ret JNICALL
#define TTFJNI_NAME(name) Java_cn_enaium_sdl_ttf_Jni_##name

// ---------------------------------------------------------------------------
// Marshaling helpers
// ---------------------------------------------------------------------------

static inline jstring ttf_jni_to_string(JNIEnv *env, const char *s) {
    return s ? env->NewStringUTF(s) : nullptr;
}

// Converts a Kotlin String to UTF-8. GetStringUTFChars() returns Modified
// UTF-8 (surrogate pairs are encoded in CESU-8), which is NOT what SDL_ttf
// expects, so the UTF-16 code units are converted properly (supplementary
// plane characters such as emoji become 4-byte UTF-8 sequences). The length
// conventions of SDL_ttf are in UTF-8 bytes; use ttf_jni_string_length() for
// the byte length of a converted string.
static inline std::string ttf_jni_copy_string(JNIEnv *env, jstring s) {
    if (!s) return std::string();
    const jsize len = env->GetStringLength(s);
    if (len == 0) return std::string();
    const jchar *chars = env->GetStringChars(s, nullptr);
    if (!chars) return std::string();
    std::string out;
    out.reserve(static_cast<size_t>(len));
    for (jsize i = 0; i < len; ++i) {
        uint32_t cp = chars[i];
        if (cp >= 0xD800 && cp <= 0xDBFF && i + 1 < len) {
            const uint32_t lo = chars[i + 1];
            if (lo >= 0xDC00 && lo <= 0xDFFF) {
                cp = 0x10000 + ((cp - 0xD800) << 10) + (lo - 0xDC00);
                ++i;
            }
        }
        if (cp < 0x80) {
            out.push_back(static_cast<char>(cp));
        } else if (cp < 0x800) {
            out.push_back(static_cast<char>(0xC0 | (cp >> 6)));
            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        } else if (cp < 0x10000) {
            out.push_back(static_cast<char>(0xE0 | (cp >> 12)));
            out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        } else {
            out.push_back(static_cast<char>(0xF0 | (cp >> 18)));
            out.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3F)));
            out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        }
    }
    env->ReleaseStringChars(s, chars);
    return out;
}

// SDL_ttf takes a byte length where 0 means "null terminated". The Kotlin
// side always passes the whole string, so compute the real UTF-8 byte length
// instead of relying on the null-terminated convention (which is unreliable
// in some SDL_ttf builds).
static inline size_t ttf_jni_string_length(jlong length, const std::string &t) {
    return length > 0 ? static_cast<size_t>(length) : t.size();
}

static inline jintArray ttf_jni_new_jint_array(JNIEnv *env, const std::vector<jint> &values) {
    jintArray arr = env->NewIntArray(static_cast<jsize>(values.size()));
    if (arr && !values.empty()) {
        env->SetIntArrayRegion(arr, 0, static_cast<jsize>(values.size()), values.data());
    }
    return arr;
}

static inline jbyteArray ttf_jni_to_bytes(JNIEnv *env, const void *data, jsize len) {
    jbyteArray arr = env->NewByteArray(len);
    if (arr && data && len > 0) {
        env->SetByteArrayRegion(arr, 0, len, static_cast<const jbyte *>(data));
    }
    return arr;
}

static inline TTF_Font *ttf_jni_font(jlong ptr) {
    return reinterpret_cast<TTF_Font *>(static_cast<intptr_t>(ptr));
}

static inline TTF_TextEngine *ttf_jni_engine(jlong ptr) {
    return reinterpret_cast<TTF_TextEngine *>(static_cast<intptr_t>(ptr));
}

static inline TTF_Text *ttf_jni_text(jlong ptr) {
    return reinterpret_cast<TTF_Text *>(static_cast<intptr_t>(ptr));
}

static inline jlong ttf_jni_ptr(const void *p) {
    return static_cast<jlong>(reinterpret_cast<intptr_t>(p));
}

static inline void ttf_jni_fill_color(SDL_Color &c, jint r, jint g, jint b, jint a) {
    c.r = static_cast<Uint8>(r);
    c.g = static_cast<Uint8>(g);
    c.b = static_cast<Uint8>(b);
    c.a = static_cast<Uint8>(a);
}

// Substring marshaling: [flags, offset, length, line_index, cluster_index,
// rect.x, rect.y, rect.w, rect.h].
static inline jintArray ttf_jni_substring(JNIEnv *env, const TTF_SubString &s) {
    return ttf_jni_new_jint_array(env, {
        static_cast<jint>(s.flags),
        s.offset,
        s.length,
        s.line_index,
        s.cluster_index,
        s.rect.x,
        s.rect.y,
        s.rect.w,
        s.rect.h,
    });
}

// ---------------------------------------------------------------------------
// Core
// ---------------------------------------------------------------------------

TTFJNI_FUNC(jboolean) TTFJNI_NAME(init)(JNIEnv *, jclass) {
    return TTF_Init() ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(void) TTFJNI_NAME(quit)(JNIEnv *, jclass) {
    TTF_Quit();
}

TTFJNI_FUNC(jint) TTFJNI_NAME(wasInit)(JNIEnv *, jclass) {
    return TTF_WasInit();
}

TTFJNI_FUNC(jint) TTFJNI_NAME(version)(JNIEnv *, jclass) {
    return TTF_Version();
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getFreeTypeVersion)(JNIEnv *env, jclass) {
    int major = 0, minor = 0, patch = 0;
    TTF_GetFreeTypeVersion(&major, &minor, &patch);
    return ttf_jni_new_jint_array(env, {major, minor, patch});
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getHarfBuzzVersion)(JNIEnv *env, jclass) {
    int major = 0, minor = 0, patch = 0;
    TTF_GetHarfBuzzVersion(&major, &minor, &patch);
    return ttf_jni_new_jint_array(env, {major, minor, patch});
}

TTFJNI_FUNC(jstring) TTFJNI_NAME(getError)(JNIEnv *env, jclass) {
    return ttf_jni_to_string(env, SDL_GetError());
}

TTFJNI_FUNC(void) TTFJNI_NAME(clearError)(JNIEnv *, jclass) {
    SDL_ClearError();
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setError)(JNIEnv *env, jclass, jstring message) {
    std::string msg = ttf_jni_copy_string(env, message);
    return SDL_SetError("%s", msg.c_str()) ? JNI_TRUE : JNI_FALSE;
}

// ---------------------------------------------------------------------------
// Fonts
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// In-memory font stream
//
// openFont reads the font file into memory and opens it from an SDL_IOStream
// backed by a custom interface whose close callback frees the buffer. The
// file-stream path (SDL_IOFromFile + freetype's seek/read pattern) is
// unreliable in a second SDL3 copy on Windows; a memory stream is pure
// pointer arithmetic and works identically on every platform.
//
// SDL_IOFromMem/SDL_IOFromConstMem do NOT copy the caller's buffer, so they
// cannot own it; this interface does.
// ---------------------------------------------------------------------------

struct TTFJniFontBuffer {
    Uint8 *data;
    Sint64 size;
    Sint64 pos;
};

static Sint64 SDLCALL ttf_jni_fontbuf_size(void *userdata) {
    return ((TTFJniFontBuffer *)userdata)->size;
}

static Sint64 SDLCALL ttf_jni_fontbuf_seek(void *userdata, Sint64 offset, SDL_IOWhence whence) {
    TTFJniFontBuffer *b = (TTFJniFontBuffer *)userdata;
    const Sint64 base = whence == SDL_IO_SEEK_SET ? 0 :
                        whence == SDL_IO_SEEK_CUR ? b->pos :
                        whence == SDL_IO_SEEK_END ? b->size : -1;
    if (base < 0) {
        return SDL_SetError("Unknown seek whence");
    }
    const Sint64 target = base + offset;
    if (target < 0 || target > b->size) {
        return SDL_SetError("Seek past end of stream");
    }
    b->pos = target;
    return target;
}

static size_t SDLCALL ttf_jni_fontbuf_read(void *userdata, void *ptr, size_t size, SDL_IOStatus *status) {
    TTFJniFontBuffer *b = (TTFJniFontBuffer *)userdata;
    if (b->pos >= b->size) {
        if (status) {
            *status = SDL_IO_STATUS_EOF;
        }
        return 0;
    }
    const size_t avail = static_cast<size_t>(b->size - b->pos);
    const size_t n = size < avail ? size : avail;
    SDL_memcpy(ptr, b->data + b->pos, n);
    b->pos += static_cast<Sint64>(n);
    return n;
}

static bool SDLCALL ttf_jni_fontbuf_close(void *userdata) {
    TTFJniFontBuffer *b = (TTFJniFontBuffer *)userdata;
    SDL_free(b->data);
    SDL_free(b);
    return true;
}

// A fresh font's first shaping/measurement call can fail once on Windows
// (SDL reports "Out of memory", likely a lazy-init glitch in the second SDL3
// copy); every retry succeeds. Perform one throwaway measurement so the
// first user-visible call always works.
static void ttf_jni_warmup_font(TTF_Font *font) {
    // A representative multi-character string with kerning, so the warm-up
    // exercises the same shaping path as typical user calls.
    int w = 0, h = 0;
    TTF_GetStringSize(font, "Hello, world!", 13, &w, &h);
    SDL_ClearError();
}

TTFJNI_FUNC(jlong) TTFJNI_NAME(openFont)(JNIEnv *env, jclass, jstring path, jfloat ptsize) {
    std::string p = ttf_jni_copy_string(env, path);

    SDL_IOStream *stream = SDL_IOFromFile(p.c_str(), "rb");
    if (!stream) {
        return 0;
    }
    const Sint64 size = SDL_GetIOSize(stream);
    void *data = nullptr;
    if (size > 0) {
        data = SDL_malloc(static_cast<size_t>(size));
        if (data) {
            const size_t got = SDL_ReadIO(stream, data, static_cast<size_t>(size));
            if (got != static_cast<size_t>(size)) {
                SDL_free(data);
                data = nullptr;
            }
        }
    }
    SDL_CloseIO(stream);
    if (!data) {
        SDL_SetError("Failed to read font file into memory");
        return 0;
    }

    TTFJniFontBuffer *buf = (TTFJniFontBuffer *)SDL_malloc(sizeof(*buf));
    if (!buf) {
        SDL_free(data);
        return 0;
    }
    buf->data = (Uint8 *)data;
    buf->size = size;
    buf->pos = 0;

    SDL_IOStreamInterface iface;
    SDL_INIT_INTERFACE(&iface);
    iface.size = ttf_jni_fontbuf_size;
    iface.seek = ttf_jni_fontbuf_seek;
    iface.read = ttf_jni_fontbuf_read;
    iface.close = ttf_jni_fontbuf_close;

    SDL_IOStream *mem = SDL_OpenIO(&iface, buf);
    if (!mem) {
        ttf_jni_fontbuf_close(buf);
        return 0;
    }
    // closeio=true: the font closes the stream (and thus frees the buffer)
    // when it is closed; TTF_CopyFont shares the stream through a refcount,
    // so the buffer outlives all fonts referencing it.
    TTF_Font *font = TTF_OpenFontIO(mem, true, ptsize);
    if (font) {
        ttf_jni_warmup_font(font);
    }
    return ttf_jni_ptr(font);
}

TTFJNI_FUNC(jlong) TTFJNI_NAME(openFontIO)(JNIEnv *, jclass, jlong stream, jboolean closeio, jfloat ptsize) {
    auto *s = reinterpret_cast<SDL_IOStream *>(static_cast<intptr_t>(stream));
    TTF_Font *font = TTF_OpenFontIO(s, closeio == JNI_TRUE, ptsize);
    if (font) {
        ttf_jni_warmup_font(font);
    }
    return ttf_jni_ptr(font);
}

TTFJNI_FUNC(jlong) TTFJNI_NAME(copyFont)(JNIEnv *, jclass, jlong font) {
    TTF_Font *copy = TTF_CopyFont(ttf_jni_font(font));
    if (copy) {
        ttf_jni_warmup_font(copy);
    }
    return ttf_jni_ptr(copy);
}

TTFJNI_FUNC(void) TTFJNI_NAME(closeFont)(JNIEnv *, jclass, jlong font) {
    TTF_CloseFont(ttf_jni_font(font));
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setFontSize)(JNIEnv *, jclass, jlong font, jfloat ptsize) {
    return TTF_SetFontSize(ttf_jni_font(font), ptsize) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setFontSizeDPI)(JNIEnv *, jclass, jlong font, jfloat ptsize, jint hdpi, jint vdpi) {
    return TTF_SetFontSizeDPI(ttf_jni_font(font), ptsize, hdpi, vdpi) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jfloat) TTFJNI_NAME(getFontSize)(JNIEnv *, jclass, jlong font) {
    return TTF_GetFontSize(ttf_jni_font(font));
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getFontDPI)(JNIEnv *env, jclass, jlong font) {
    int hdpi = 0, vdpi = 0;
    if (!TTF_GetFontDPI(ttf_jni_font(font), &hdpi, &vdpi)) {
        return nullptr;
    }
    return ttf_jni_new_jint_array(env, {hdpi, vdpi});
}

TTFJNI_FUNC(void) TTFJNI_NAME(setFontStyle)(JNIEnv *, jclass, jlong font, jint style) {
    TTF_SetFontStyle(ttf_jni_font(font), static_cast<TTF_FontStyleFlags>(style));
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getFontStyle)(JNIEnv *, jclass, jlong font) {
    return static_cast<jint>(TTF_GetFontStyle(ttf_jni_font(font)));
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setFontOutline)(JNIEnv *, jclass, jlong font, jint outline) {
    return TTF_SetFontOutline(ttf_jni_font(font), outline) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getFontOutline)(JNIEnv *, jclass, jlong font) {
    return TTF_GetFontOutline(ttf_jni_font(font));
}

TTFJNI_FUNC(void) TTFJNI_NAME(setFontHinting)(JNIEnv *, jclass, jlong font, jint hinting) {
    TTF_SetFontHinting(ttf_jni_font(font), static_cast<TTF_HintingFlags>(hinting));
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getFontHinting)(JNIEnv *, jclass, jlong font) {
    return static_cast<jint>(TTF_GetFontHinting(ttf_jni_font(font)));
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setFontSDF)(JNIEnv *, jclass, jlong font, jboolean enabled) {
    return TTF_SetFontSDF(ttf_jni_font(font), enabled == JNI_TRUE) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(getFontSDF)(JNIEnv *, jclass, jlong font) {
    return TTF_GetFontSDF(ttf_jni_font(font)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(void) TTFJNI_NAME(setFontWrapAlignment)(JNIEnv *, jclass, jlong font, jint align) {
    TTF_SetFontWrapAlignment(ttf_jni_font(font), static_cast<TTF_HorizontalAlignment>(align));
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getFontWrapAlignment)(JNIEnv *, jclass, jlong font) {
    return static_cast<jint>(TTF_GetFontWrapAlignment(ttf_jni_font(font)));
}

TTFJNI_FUNC(void) TTFJNI_NAME(setFontLineSkip)(JNIEnv *, jclass, jlong font, jint lineskip) {
    TTF_SetFontLineSkip(ttf_jni_font(font), lineskip);
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getFontLineSkip)(JNIEnv *, jclass, jlong font) {
    return TTF_GetFontLineSkip(ttf_jni_font(font));
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getFontHeight)(JNIEnv *, jclass, jlong font) {
    return TTF_GetFontHeight(ttf_jni_font(font));
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getFontAscent)(JNIEnv *, jclass, jlong font) {
    return TTF_GetFontAscent(ttf_jni_font(font));
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getFontDescent)(JNIEnv *, jclass, jlong font) {
    return TTF_GetFontDescent(ttf_jni_font(font));
}

TTFJNI_FUNC(void) TTFJNI_NAME(setFontKerning)(JNIEnv *, jclass, jlong font, jboolean enabled) {
    TTF_SetFontKerning(ttf_jni_font(font), enabled == JNI_TRUE);
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(getFontKerning)(JNIEnv *, jclass, jlong font) {
    return TTF_GetFontKerning(ttf_jni_font(font)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(fontIsFixedWidth)(JNIEnv *, jclass, jlong font) {
    return TTF_FontIsFixedWidth(ttf_jni_font(font)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(fontIsScalable)(JNIEnv *, jclass, jlong font) {
    return TTF_FontIsScalable(ttf_jni_font(font)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jstring) TTFJNI_NAME(getFontFamilyName)(JNIEnv *env, jclass, jlong font) {
    return ttf_jni_to_string(env, TTF_GetFontFamilyName(ttf_jni_font(font)));
}

TTFJNI_FUNC(jstring) TTFJNI_NAME(getFontStyleName)(JNIEnv *env, jclass, jlong font) {
    return ttf_jni_to_string(env, TTF_GetFontStyleName(ttf_jni_font(font)));
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setFontDirection)(JNIEnv *, jclass, jlong font, jint direction) {
    return TTF_SetFontDirection(ttf_jni_font(font), static_cast<TTF_Direction>(direction)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getFontDirection)(JNIEnv *, jclass, jlong font) {
    return static_cast<jint>(TTF_GetFontDirection(ttf_jni_font(font)));
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setFontCharSpacing)(JNIEnv *, jclass, jlong font, jint spacing) {
    return TTF_SetFontCharSpacing(ttf_jni_font(font), spacing) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getFontCharSpacing)(JNIEnv *, jclass, jlong font) {
    return TTF_GetFontCharSpacing(ttf_jni_font(font));
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getFontWeight)(JNIEnv *, jclass, jlong font) {
    return TTF_GetFontWeight(ttf_jni_font(font));
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(addFallbackFont)(JNIEnv *, jclass, jlong font, jlong fallback) {
    return TTF_AddFallbackFont(ttf_jni_font(font), ttf_jni_font(fallback)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(void) TTFJNI_NAME(removeFallbackFont)(JNIEnv *, jclass, jlong font, jlong fallback) {
    TTF_RemoveFallbackFont(ttf_jni_font(font), ttf_jni_font(fallback));
}

TTFJNI_FUNC(void) TTFJNI_NAME(clearFallbackFonts)(JNIEnv *, jclass, jlong font) {
    TTF_ClearFallbackFonts(ttf_jni_font(font));
}

// ---------------------------------------------------------------------------
// Glyphs / metrics
// ---------------------------------------------------------------------------

TTFJNI_FUNC(jboolean) TTFJNI_NAME(fontHasGlyph)(JNIEnv *, jclass, jlong font, jint ch) {
    return TTF_FontHasGlyph(ttf_jni_font(font), static_cast<Uint32>(ch)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getGlyphMetrics)(JNIEnv *env, jclass, jlong font, jint ch) {
    int minx = 0, maxx = 0, miny = 0, maxy = 0, advance = 0;
    if (!TTF_GetGlyphMetrics(ttf_jni_font(font), static_cast<Uint32>(ch),
                             &minx, &maxx, &miny, &maxy, &advance)) {
        return nullptr;
    }
    return ttf_jni_new_jint_array(env, {minx, maxx, miny, maxy, advance});
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getGlyphKerning)(JNIEnv *env, jclass, jlong font, jint previous_ch, jint ch) {
    int kerning = 0;
    if (!TTF_GetGlyphKerning(ttf_jni_font(font), static_cast<Uint32>(previous_ch),
                             static_cast<Uint32>(ch), &kerning)) {
        return nullptr;
    }
    return ttf_jni_new_jint_array(env, {kerning});
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getStringSize)(JNIEnv *env, jclass, jlong font, jstring text, jlong length) {
    std::string t = ttf_jni_copy_string(env, text);
    int w = 0, h = 0;
    const bool result = TTF_GetStringSize(ttf_jni_font(font), t.c_str(), ttf_jni_string_length(length, t), &w, &h);
    fprintf(stderr, "TTFDIAG: TTF_GetStringSize(font=%p text='%s' len=%zu) result=%d w=%d h=%d err='%s'\n",
            ttf_jni_font(font), t.c_str(), ttf_jni_string_length(length, t), result, w, h,
            SDL_GetError() ? SDL_GetError() : "");
    if (!result) {
        return nullptr;
    }
    return ttf_jni_new_jint_array(env, {w, h});
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getStringSizeWrapped)(JNIEnv *env, jclass, jlong font, jstring text, jlong length, jint wrapWidth) {
    std::string t = ttf_jni_copy_string(env, text);
    int w = 0, h = 0;
    if (!TTF_GetStringSizeWrapped(ttf_jni_font(font), t.c_str(), ttf_jni_string_length(length, t), wrapWidth, &w, &h)) {
        return nullptr;
    }
    return ttf_jni_new_jint_array(env, {w, h});
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(measureString)(JNIEnv *env, jclass, jlong font, jstring text, jlong length, jint maxWidth) {
    std::string t = ttf_jni_copy_string(env, text);
    int measuredWidth = 0;
    size_t measuredLength = 0;
    if (!TTF_MeasureString(ttf_jni_font(font), t.c_str(), ttf_jni_string_length(length, t),
                           maxWidth, &measuredWidth, &measuredLength)) {
        return nullptr;
    }
    return ttf_jni_new_jint_array(env, {measuredWidth, static_cast<jint>(measuredLength)});
}

// ---------------------------------------------------------------------------
// Rendering to surfaces
//
// The returned jlong is an SDL_Surface handle allocated by SDL_ttf; the
// Kotlin bindings wrap it with sdl-kmp's SDL.surfaceFromPtr(owned = true) so
// it can be used directly with the sdl-kmp renderer API.
// ---------------------------------------------------------------------------

#define TTF_RENDER_TEXT_FN(NAME, CALL)                                                  \
    TTFJNI_FUNC(jlong) TTFJNI_NAME(NAME)(JNIEnv *env, jclass, jlong font, jstring text, \
                                         jlong length, jint r, jint g, jint b, jint a) { \
        std::string t = ttf_jni_copy_string(env, text);                                 \
        SDL_Color fg;                                                                   \
        ttf_jni_fill_color(fg, r, g, b, a);                                             \
        SDL_Surface *surf = CALL(ttf_jni_font(font), t.c_str(),                          \
                                ttf_jni_string_length(length, t), fg);                      \
        fprintf(stderr, "TTFDIAG: %s(font=%p text='%s' len=%zu) surf=%p err='%s'\n",       \
                #NAME, ttf_jni_font(font), t.c_str(), ttf_jni_string_length(length, t),     \
                (void *)surf, SDL_GetError() ? SDL_GetError() : "");                        \
        return ttf_jni_ptr(surf);                                                           \
    }

#define TTF_RENDER_TEXT_BG_FN(NAME, CALL)                                               \
    TTFJNI_FUNC(jlong) TTFJNI_NAME(NAME)(JNIEnv *env, jclass, jlong font, jstring text, \
                                         jlong length, jint r, jint g, jint b, jint a,   \
                                         jint br, jint bg, jint bb, jint ba) {           \
        std::string t = ttf_jni_copy_string(env, text);                                 \
        SDL_Color fg, bgc;                                                              \
        ttf_jni_fill_color(fg, r, g, b, a);                                             \
        ttf_jni_fill_color(bgc, br, bg, bb, ba);                                        \
        SDL_Surface *surf = CALL(ttf_jni_font(font), t.c_str(),                          \
                                ttf_jni_string_length(length, t), fg, bgc);             \
        fprintf(stderr, "TTFDIAG: %s(font=%p text='%s' len=%zu) surf=%p err='%s'\n",    \
                #NAME, ttf_jni_font(font), t.c_str(), ttf_jni_string_length(length, t),  \
                (void *)surf, SDL_GetError() ? SDL_GetError() : "");                     \
        return ttf_jni_ptr(surf);                                                        \
    }

#define TTF_RENDER_TEXT_WRAPPED_FN(NAME, CALL)                                          \
    TTFJNI_FUNC(jlong) TTFJNI_NAME(NAME)(JNIEnv *env, jclass, jlong font, jstring text, \
                                         jlong length, jint r, jint g, jint b, jint a,   \
                                         jint wrapWidth) {                              \
        std::string t = ttf_jni_copy_string(env, text);                                 \
        SDL_Color fg;                                                                   \
        ttf_jni_fill_color(fg, r, g, b, a);                                             \
        return ttf_jni_ptr(CALL(ttf_jni_font(font), t.c_str(),                          \
                                ttf_jni_string_length(length, t), fg, wrapWidth));           \
    }

#define TTF_RENDER_TEXT_BG_WRAPPED_FN(NAME, CALL)                                       \
    TTFJNI_FUNC(jlong) TTFJNI_NAME(NAME)(JNIEnv *env, jclass, jlong font, jstring text, \
                                         jlong length, jint r, jint g, jint b, jint a,   \
                                         jint br, jint bg, jint bb, jint ba,             \
                                         jint wrapWidth) {                              \
        std::string t = ttf_jni_copy_string(env, text);                                 \
        SDL_Color fg, bgc;                                                              \
        ttf_jni_fill_color(fg, r, g, b, a);                                             \
        ttf_jni_fill_color(bgc, br, bg, bb, ba);                                        \
        return ttf_jni_ptr(CALL(ttf_jni_font(font), t.c_str(),                          \
                                ttf_jni_string_length(length, t), fg, bgc, wrapWidth));      \
    }

TTF_RENDER_TEXT_FN(renderTextSolid, TTF_RenderText_Solid)
TTF_RENDER_TEXT_FN(renderTextBlended, TTF_RenderText_Blended)
TTF_RENDER_TEXT_WRAPPED_FN(renderTextSolidWrapped, TTF_RenderText_Solid_Wrapped)
TTF_RENDER_TEXT_WRAPPED_FN(renderTextBlendedWrapped, TTF_RenderText_Blended_Wrapped)
TTF_RENDER_TEXT_BG_FN(renderTextShaded, TTF_RenderText_Shaded)
TTF_RENDER_TEXT_BG_FN(renderTextLCD, TTF_RenderText_LCD)
TTF_RENDER_TEXT_BG_WRAPPED_FN(renderTextShadedWrapped, TTF_RenderText_Shaded_Wrapped)
TTF_RENDER_TEXT_BG_WRAPPED_FN(renderTextLCDWrapped, TTF_RenderText_LCD_Wrapped)

#define TTF_RENDER_GLYPH_FN(NAME, CALL)                                                   \
    TTFJNI_FUNC(jlong) TTFJNI_NAME(NAME)(JNIEnv *, jclass, jlong font, jint ch,          \
                                         jint r, jint g, jint b, jint a) {                 \
        SDL_Color fg;                                                                     \
        ttf_jni_fill_color(fg, r, g, b, a);                                               \
        return ttf_jni_ptr(CALL(ttf_jni_font(font), static_cast<Uint32>(ch), fg));        \
    }

#define TTF_RENDER_GLYPH_BG_FN(NAME, CALL)                                                \
    TTFJNI_FUNC(jlong) TTFJNI_NAME(NAME)(JNIEnv *, jclass, jlong font, jint ch,          \
                                         jint r, jint g, jint b, jint a,                  \
                                         jint br, jint bg, jint bb, jint ba) {            \
        SDL_Color fg, bgc;                                                                \
        ttf_jni_fill_color(fg, r, g, b, a);                                               \
        ttf_jni_fill_color(bgc, br, bg, bb, ba);                                          \
        return ttf_jni_ptr(CALL(ttf_jni_font(font), static_cast<Uint32>(ch), fg, bgc));   \
    }

TTF_RENDER_GLYPH_FN(renderGlyphSolid, TTF_RenderGlyph_Solid)
TTF_RENDER_GLYPH_FN(renderGlyphBlended, TTF_RenderGlyph_Blended)
TTF_RENDER_GLYPH_BG_FN(renderGlyphShaded, TTF_RenderGlyph_Shaded)
TTF_RENDER_GLYPH_BG_FN(renderGlyphLCD, TTF_RenderGlyph_LCD)

TTFJNI_FUNC(jlong) TTFJNI_NAME(getGlyphImage)(JNIEnv *, jclass, jlong font, jint ch) {
    TTF_ImageType imageType = TTF_IMAGE_INVALID;
    return ttf_jni_ptr(TTF_GetGlyphImage(ttf_jni_font(font), static_cast<Uint32>(ch), &imageType));
}

TTFJNI_FUNC(jlong) TTFJNI_NAME(getGlyphImageForIndex)(JNIEnv *, jclass, jlong font, jint glyphIndex) {
    TTF_ImageType imageType = TTF_IMAGE_INVALID;
    return ttf_jni_ptr(TTF_GetGlyphImageForIndex(ttf_jni_font(font), static_cast<Uint32>(glyphIndex), &imageType));
}

// ---------------------------------------------------------------------------
// Surfaces
//
// These wrap the SDL_Surface objects allocated by SDL_ttf's render functions
// into the sdl-kmp SDLSurface interface (see Long.toSDLSurface in the Kotlin
// bindings). The SDL calls are resolved at runtime from libsdl_jni, which
// owns the SDL3 instance the surfaces were created with.
// ---------------------------------------------------------------------------

static inline SDL_Surface *ttf_jni_surface(jlong ptr) {
    return reinterpret_cast<SDL_Surface *>(static_cast<intptr_t>(ptr));
}

TTFJNI_FUNC(jint) TTFJNI_NAME(surfaceWidth)(JNIEnv *, jclass, jlong surface) {
    return ttf_jni_surface(surface)->w;
}

TTFJNI_FUNC(jint) TTFJNI_NAME(surfaceHeight)(JNIEnv *, jclass, jlong surface) {
    return ttf_jni_surface(surface)->h;
}

TTFJNI_FUNC(jint) TTFJNI_NAME(surfaceFormat)(JNIEnv *, jclass, jlong surface) {
    return static_cast<jint>(ttf_jni_surface(surface)->format);
}

TTFJNI_FUNC(jint) TTFJNI_NAME(surfacePitch)(JNIEnv *, jclass, jlong surface) {
    return ttf_jni_surface(surface)->pitch;
}

TTFJNI_FUNC(jint) TTFJNI_NAME(surfaceColorspace)(JNIEnv *, jclass, jlong surface) {
    return static_cast<jint>(SDL_GetSurfaceColorspace(ttf_jni_surface(surface)));
}

TTFJNI_FUNC(jbyteArray) TTFJNI_NAME(surfacePixels)(JNIEnv *env, jclass, jlong surface) {
    SDL_Surface *s = ttf_jni_surface(surface);
    const int bytes = s->pitch * s->h;
    return ttf_jni_to_bytes(env, s->pixels, bytes);
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(lockSurface)(JNIEnv *, jclass, jlong surface) {
    return SDL_LockSurface(ttf_jni_surface(surface)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(void) TTFJNI_NAME(unlockSurface)(JNIEnv *, jclass, jlong surface) {
    SDL_UnlockSurface(ttf_jni_surface(surface));
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(surfaceFillRect)(JNIEnv *env, jclass, jlong surface,
                                                   jintArray rect, jint r, jint g, jint b, jint a) {
    SDL_Surface *s = ttf_jni_surface(surface);
    SDL_Rect rct;
    SDL_Rect *rctPtr = nullptr;
    if (rect) {
        jint *elems = env->GetIntArrayElements(rect, nullptr);
        rct.x = elems[0];
        rct.y = elems[1];
        rct.w = elems[2];
        rct.h = elems[3];
        env->ReleaseIntArrayElements(rect, elems, JNI_ABORT);
        rctPtr = &rct;
    }
    return SDL_FillSurfaceRect(s, rctPtr, static_cast<Uint32>((r << 24) | (g << 16) | (b << 8) | a)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(surfaceFillRects)(JNIEnv *env, jclass, jlong surface,
                                                    jintArray rects, jint r, jint g, jint b, jint a) {
    SDL_Surface *s = ttf_jni_surface(surface);
    jsize count = env->GetArrayLength(rects) / 4;
    std::vector<SDL_Rect> rct(static_cast<size_t>(count));
    jint *elems = env->GetIntArrayElements(rects, nullptr);
    for (jsize i = 0; i < count; i++) {
        rct[static_cast<size_t>(i)].x = elems[i * 4];
        rct[static_cast<size_t>(i)].y = elems[i * 4 + 1];
        rct[static_cast<size_t>(i)].w = elems[i * 4 + 2];
        rct[static_cast<size_t>(i)].h = elems[i * 4 + 3];
    }
    env->ReleaseIntArrayElements(rects, elems, JNI_ABORT);
    return SDL_FillSurfaceRects(s, rct.data(), count, static_cast<Uint32>((r << 24) | (g << 16) | (b << 8) | a)) ? JNI_TRUE : JNI_FALSE;
}

static inline bool ttf_jni_read_rect(JNIEnv *env, jintArray arr, SDL_Rect &out) {
    if (!arr) return false;
    jint *elems = env->GetIntArrayElements(arr, nullptr);
    out.x = elems[0];
    out.y = elems[1];
    out.w = elems[2];
    out.h = elems[3];
    env->ReleaseIntArrayElements(arr, elems, JNI_ABORT);
    return true;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(surfaceBlit)(JNIEnv *env, jclass, jlong src, jintArray srcRect,
                                               jlong dst, jintArray dstRect) {
    SDL_Rect sr, dr;
    return SDL_BlitSurface(ttf_jni_surface(src),
                           ttf_jni_read_rect(env, srcRect, sr) ? &sr : nullptr,
                           ttf_jni_surface(dst),
                           ttf_jni_read_rect(env, dstRect, dr) ? &dr : nullptr) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(surfaceBlitScaled)(JNIEnv *env, jclass, jlong src, jintArray srcRect,
                                                     jlong dst, jintArray dstRect, jint scaleMode) {
    SDL_Rect sr, dr;
    return SDL_BlitSurfaceScaled(ttf_jni_surface(src),
                                 ttf_jni_read_rect(env, srcRect, sr) ? &sr : nullptr,
                                 ttf_jni_surface(dst),
                                 ttf_jni_read_rect(env, dstRect, dr) ? &dr : nullptr,
                                 static_cast<SDL_ScaleMode>(scaleMode)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(surfaceSaveBMP)(JNIEnv *env, jclass, jlong surface, jstring path) {
    std::string p = ttf_jni_copy_string(env, path);
    return SDL_SaveBMP(ttf_jni_surface(surface), p.c_str()) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jlong) TTFJNI_NAME(convertSurface)(JNIEnv *, jclass, jlong surface, jint format) {
    return ttf_jni_ptr(SDL_ConvertSurface(ttf_jni_surface(surface), static_cast<SDL_PixelFormat>(format)));
}

TTFJNI_FUNC(void) TTFJNI_NAME(destroySurface)(JNIEnv *, jclass, jlong surface) {
    SDL_DestroySurface(ttf_jni_surface(surface));
}

// ---------------------------------------------------------------------------
// Text engines
// ---------------------------------------------------------------------------

TTFJNI_FUNC(jlong) TTFJNI_NAME(createRendererTextEngine)(JNIEnv *, jclass, jlong renderer) {
    auto *r = reinterpret_cast<SDL_Renderer *>(static_cast<intptr_t>(renderer));
    return ttf_jni_ptr(TTF_CreateRendererTextEngine(r));
}

TTFJNI_FUNC(jlong) TTFJNI_NAME(createSurfaceTextEngine)(JNIEnv *, jclass) {
    return ttf_jni_ptr(TTF_CreateSurfaceTextEngine());
}

TTFJNI_FUNC(void) TTFJNI_NAME(destroyRendererTextEngine)(JNIEnv *, jclass, jlong engine) {
    TTF_DestroyRendererTextEngine(ttf_jni_engine(engine));
}

TTFJNI_FUNC(void) TTFJNI_NAME(destroySurfaceTextEngine)(JNIEnv *, jclass, jlong engine) {
    TTF_DestroySurfaceTextEngine(ttf_jni_engine(engine));
}

// ---------------------------------------------------------------------------
// Text objects
// ---------------------------------------------------------------------------

TTFJNI_FUNC(jlong) TTFJNI_NAME(createText)(JNIEnv *env, jclass, jlong engine, jlong font, jstring text, jlong length) {
    std::string t = ttf_jni_copy_string(env, text);
    return ttf_jni_ptr(TTF_CreateText(ttf_jni_engine(engine), ttf_jni_font(font),
                                      t.c_str(), ttf_jni_string_length(length, t)));
}

TTFJNI_FUNC(void) TTFJNI_NAME(destroyText)(JNIEnv *, jclass, jlong text) {
    TTF_DestroyText(ttf_jni_text(text));
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setTextEngine)(JNIEnv *, jclass, jlong text, jlong engine) {
    return TTF_SetTextEngine(ttf_jni_text(text), ttf_jni_engine(engine)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setTextFont)(JNIEnv *, jclass, jlong text, jlong font) {
    return TTF_SetTextFont(ttf_jni_text(text), ttf_jni_font(font)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setTextDirection)(JNIEnv *, jclass, jlong text, jint direction) {
    return TTF_SetTextDirection(ttf_jni_text(text), static_cast<TTF_Direction>(direction)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getTextDirection)(JNIEnv *, jclass, jlong text) {
    return static_cast<jint>(TTF_GetTextDirection(ttf_jni_text(text)));
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setTextColor)(JNIEnv *, jclass, jlong text, jint r, jint g, jint b, jint a) {
    return TTF_SetTextColor(ttf_jni_text(text), static_cast<Uint8>(r), static_cast<Uint8>(g),
                            static_cast<Uint8>(b), static_cast<Uint8>(a)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getTextColor)(JNIEnv *env, jclass, jlong text) {
    Uint8 r = 0, g = 0, b = 0, a = 0;
    if (!TTF_GetTextColor(ttf_jni_text(text), &r, &g, &b, &a)) {
        return nullptr;
    }
    return ttf_jni_new_jint_array(env, {r, g, b, a});
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setTextPosition)(JNIEnv *, jclass, jlong text, jint x, jint y) {
    return TTF_SetTextPosition(ttf_jni_text(text), x, y) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getTextPosition)(JNIEnv *env, jclass, jlong text) {
    int x = 0, y = 0;
    if (!TTF_GetTextPosition(ttf_jni_text(text), &x, &y)) {
        return nullptr;
    }
    return ttf_jni_new_jint_array(env, {x, y});
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setTextWrapWidth)(JNIEnv *, jclass, jlong text, jint wrapWidth) {
    return TTF_SetTextWrapWidth(ttf_jni_text(text), wrapWidth) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getTextWrapWidth)(JNIEnv *, jclass, jlong text) {
    int wrapWidth = 0;
    TTF_GetTextWrapWidth(ttf_jni_text(text), &wrapWidth);
    return wrapWidth;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setTextWrapWhitespaceVisible)(JNIEnv *, jclass, jlong text, jboolean visible) {
    return TTF_SetTextWrapWhitespaceVisible(ttf_jni_text(text), visible == JNI_TRUE) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(textWrapWhitespaceVisible)(JNIEnv *, jclass, jlong text) {
    return TTF_TextWrapWhitespaceVisible(ttf_jni_text(text)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(setTextString)(JNIEnv *env, jclass, jlong text, jstring string, jlong length) {
    std::string t = ttf_jni_copy_string(env, string);
    return TTF_SetTextString(ttf_jni_text(text), t.c_str(), ttf_jni_string_length(length, t)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(appendTextString)(JNIEnv *env, jclass, jlong text, jstring string, jlong length) {
    std::string t = ttf_jni_copy_string(env, string);
    return TTF_AppendTextString(ttf_jni_text(text), t.c_str(), ttf_jni_string_length(length, t)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(insertTextString)(JNIEnv *env, jclass, jlong text, jint offset, jstring string, jlong length) {
    std::string t = ttf_jni_copy_string(env, string);
    return TTF_InsertTextString(ttf_jni_text(text), offset, t.c_str(), ttf_jni_string_length(length, t)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(deleteTextString)(JNIEnv *, jclass, jlong text, jint offset, jint length) {
    return TTF_DeleteTextString(ttf_jni_text(text), offset, length) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jstring) TTFJNI_NAME(getTextString)(JNIEnv *env, jclass, jlong text) {
    TTF_Text *t = ttf_jni_text(text);
    return t ? ttf_jni_to_string(env, t->text) : nullptr;
}

TTFJNI_FUNC(jint) TTFJNI_NAME(getTextNumLines)(JNIEnv *, jclass, jlong text) {
    TTF_Text *t = ttf_jni_text(text);
    return t ? t->num_lines : 0;
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getTextSize)(JNIEnv *env, jclass, jlong text) {
    int w = 0, h = 0;
    if (!TTF_GetTextSize(ttf_jni_text(text), &w, &h)) {
        return nullptr;
    }
    return ttf_jni_new_jint_array(env, {w, h});
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(drawRendererText)(JNIEnv *, jclass, jlong text, jfloat x, jfloat y) {
    return TTF_DrawRendererText(ttf_jni_text(text), x, y) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(drawSurfaceText)(JNIEnv *, jclass, jlong text, jint x, jint y, jlong surface) {
    auto *s = reinterpret_cast<SDL_Surface *>(static_cast<intptr_t>(surface));
    return TTF_DrawSurfaceText(ttf_jni_text(text), x, y, s) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jboolean) TTFJNI_NAME(updateText)(JNIEnv *, jclass, jlong text) {
    return TTF_UpdateText(ttf_jni_text(text)) ? JNI_TRUE : JNI_FALSE;
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getTextSubString)(JNIEnv *env, jclass, jlong text, jint offset) {
    TTF_SubString substring;
    if (!TTF_GetTextSubString(ttf_jni_text(text), offset, &substring)) {
        return nullptr;
    }
    return ttf_jni_substring(env, substring);
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getTextSubStringForLine)(JNIEnv *env, jclass, jlong text, jint line) {
    TTF_SubString substring;
    if (!TTF_GetTextSubStringForLine(ttf_jni_text(text), line, &substring)) {
        return nullptr;
    }
    return ttf_jni_substring(env, substring);
}

TTFJNI_FUNC(jintArray) TTFJNI_NAME(getTextSubStringForPoint)(JNIEnv *env, jclass, jlong text, jint x, jint y) {
    TTF_SubString substring;
    if (!TTF_GetTextSubStringForPoint(ttf_jni_text(text), x, y, &substring)) {
        return nullptr;
    }
    return ttf_jni_substring(env, substring);
}
