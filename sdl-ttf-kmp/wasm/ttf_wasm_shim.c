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
 * Flat, handle-based wrapper around SDL_ttf for Kotlin/Wasm.
 *
 * Kotlin/Wasm has no cinterop and no raw memory access, so this shim hides
 * every SDL_ttf pointer behind a 32-bit handle and exposes the API used by
 * sdl-ttf-kmp as simple functions taking/returning ints, floats and strings.
 * It is compiled with Emscripten together with SDL3 and SDL_ttf; the JS glue
 * layer (ttf_kmp_glue.js) marshals Kotlin <-> C (strings and output buffers).
 *
 * The module also contains the sdl-kmp SDL3 shim (sdl_wasm_shim.c, vendored
 * unchanged from sdl-kmp), so SDL_Surface handles produced by SDL_ttf are
 * valid for the sdl-kmp surface / renderer bindings.
 *
 * Output buffers are int arrays in the Emscripten heap; the JS glue copies
 * them into the shared sdlKmpResultI32 slots after the call.
 *
 * String convention: strings returned by SDL_ttf (font family/style names,
 * TTF_Text->text) are owned by the object and never freed here.
 */

#include <SDL3/SDL.h>
#include <SDL3_ttf/SDL_ttf.h>

#include <stdint.h>

#define KMP_HANDLE(ptr) ((int)(uintptr_t)(ptr))
#define KMP_PTR(type, handle) ((type *)(uintptr_t)(uint32_t)(handle))

static SDL_Color kmp_color(int r, int g, int b, int a)
{
    SDL_Color color;
    color.r = (Uint8)r;
    color.g = (Uint8)g;
    color.b = (Uint8)b;
    color.a = (Uint8)a;
    return color;
}

/* SDL_ttf renders into INDEX8 (Solid/Shaded) and ARGB8888 (Blended/LCD)
 * surfaces. Normalize them to the endianness-independent RGBA32, matching
 * the JVM/native bindings, so the pixel bytes can be uploaded straight into
 * an R8G8B8A8 texture. The source surface is destroyed on conversion. */
static int kmp_rgba32(SDL_Surface *surface)
{
    if (surface == NULL || surface->format == SDL_PIXELFORMAT_RGBA32) {
        return KMP_HANDLE(surface);
    }
    SDL_Surface *converted = SDL_ConvertSurface(surface, SDL_PIXELFORMAT_RGBA32);
    SDL_DestroySurface(surface);
    return KMP_HANDLE(converted);
}

/* =========================================================================
 * Library
 * ========================================================================= */

int sdl_ttf_kmp_Init(void) { return TTF_Init(); }
void sdl_ttf_kmp_Quit(void) { TTF_Quit(); }
int sdl_ttf_kmp_WasInit(void) { return TTF_WasInit(); }
int sdl_ttf_kmp_Version(void) { return TTF_Version(); }

void sdl_ttf_kmp_GetFreeTypeVersion(int *out)
{
    if (out == NULL) {
        return;
    }
    TTF_GetFreeTypeVersion(&out[0], &out[1], &out[2]);
}

void sdl_ttf_kmp_GetHarfBuzzVersion(int *out)
{
    if (out == NULL) {
        return;
    }
    TTF_GetHarfBuzzVersion(&out[0], &out[1], &out[2]);
}

/* =========================================================================
 * Fonts
 * ========================================================================= */

int sdl_ttf_kmp_OpenFont(const char *path, float point_size)
{
    return KMP_HANDLE(TTF_OpenFont(path, point_size));
}

int sdl_ttf_kmp_OpenFontIO(int stream, int close_io, float point_size)
{
    return KMP_HANDLE(TTF_OpenFontIO(KMP_PTR(SDL_IOStream, stream), close_io != 0, point_size));
}

int sdl_ttf_kmp_CopyFont(int font)
{
    return KMP_HANDLE(TTF_CopyFont(KMP_PTR(TTF_Font, font)));
}

void sdl_ttf_kmp_CloseFont(int font)
{
    TTF_CloseFont(KMP_PTR(TTF_Font, font));
}

float sdl_ttf_kmp_GetFontSize(int font) { return TTF_GetFontSize(KMP_PTR(TTF_Font, font)); }
int sdl_ttf_kmp_SetFontSize(int font, float point_size) { return TTF_SetFontSize(KMP_PTR(TTF_Font, font), point_size); }
int sdl_ttf_kmp_GetFontStyle(int font) { return (int)TTF_GetFontStyle(KMP_PTR(TTF_Font, font)); }
void sdl_ttf_kmp_SetFontStyle(int font, int style) { TTF_SetFontStyle(KMP_PTR(TTF_Font, font), (TTF_FontStyleFlags)style); }
int sdl_ttf_kmp_GetFontOutline(int font) { return TTF_GetFontOutline(KMP_PTR(TTF_Font, font)); }
void sdl_ttf_kmp_SetFontOutline(int font, int outline) { TTF_SetFontOutline(KMP_PTR(TTF_Font, font), outline); }
int sdl_ttf_kmp_GetFontHinting(int font) { return (int)TTF_GetFontHinting(KMP_PTR(TTF_Font, font)); }
void sdl_ttf_kmp_SetFontHinting(int font, int hinting) { TTF_SetFontHinting(KMP_PTR(TTF_Font, font), (TTF_HintingFlags)hinting); }
int sdl_ttf_kmp_GetFontSDF(int font) { return TTF_GetFontSDF(KMP_PTR(TTF_Font, font)); }
int sdl_ttf_kmp_SetFontSDF(int font, int enabled) { return TTF_SetFontSDF(KMP_PTR(TTF_Font, font), enabled != 0); }
int sdl_ttf_kmp_GetFontWrapAlignment(int font) { return (int)TTF_GetFontWrapAlignment(KMP_PTR(TTF_Font, font)); }
void sdl_ttf_kmp_SetFontWrapAlignment(int font, int align) { TTF_SetFontWrapAlignment(KMP_PTR(TTF_Font, font), (TTF_HorizontalAlignment)align); }
int sdl_ttf_kmp_GetFontLineSkip(int font) { return TTF_GetFontLineSkip(KMP_PTR(TTF_Font, font)); }
void sdl_ttf_kmp_SetFontLineSkip(int font, int line_skip) { TTF_SetFontLineSkip(KMP_PTR(TTF_Font, font), line_skip); }
int sdl_ttf_kmp_GetFontKerning(int font) { return TTF_GetFontKerning(KMP_PTR(TTF_Font, font)); }
void sdl_ttf_kmp_SetFontKerning(int font, int enabled) { TTF_SetFontKerning(KMP_PTR(TTF_Font, font), enabled != 0); }
int sdl_ttf_kmp_GetFontDirection(int font) { return (int)TTF_GetFontDirection(KMP_PTR(TTF_Font, font)); }
void sdl_ttf_kmp_SetFontDirection(int font, int direction) { TTF_SetFontDirection(KMP_PTR(TTF_Font, font), (TTF_Direction)direction); }

int sdl_ttf_kmp_GetFontHeight(int font) { return TTF_GetFontHeight(KMP_PTR(TTF_Font, font)); }
int sdl_ttf_kmp_GetFontAscent(int font) { return TTF_GetFontAscent(KMP_PTR(TTF_Font, font)); }
int sdl_ttf_kmp_GetFontDescent(int font) { return TTF_GetFontDescent(KMP_PTR(TTF_Font, font)); }
const char *sdl_ttf_kmp_GetFontFamilyName(int font) { return TTF_GetFontFamilyName(KMP_PTR(TTF_Font, font)); }
const char *sdl_ttf_kmp_GetFontStyleName(int font) { return TTF_GetFontStyleName(KMP_PTR(TTF_Font, font)); }
int sdl_ttf_kmp_FontIsFixedWidth(int font) { return TTF_FontIsFixedWidth(KMP_PTR(TTF_Font, font)); }
int sdl_ttf_kmp_FontIsScalable(int font) { return TTF_FontIsScalable(KMP_PTR(TTF_Font, font)); }
int sdl_ttf_kmp_GetFontWeight(int font) { return TTF_GetFontWeight(KMP_PTR(TTF_Font, font)); }
int sdl_ttf_kmp_FontHasGlyph(int font, int ch) { return TTF_FontHasGlyph(KMP_PTR(TTF_Font, font), (Uint32)ch); }

int sdl_ttf_kmp_GetGlyphMetrics(int font, int ch, int *out)
{
    int minx = 0, maxx = 0, miny = 0, maxy = 0, advance = 0;
    int ok = TTF_GetGlyphMetrics(KMP_PTR(TTF_Font, font), (Uint32)ch, &minx, &maxx, &miny, &maxy, &advance);
    if (out != NULL) {
        out[0] = minx;
        out[1] = maxx;
        out[2] = miny;
        out[3] = maxy;
        out[4] = advance;
    }
    return ok;
}

int sdl_ttf_kmp_GetGlyphKerning(int font, int previous_ch, int ch, int *out)
{
    int kerning = 0;
    int ok = TTF_GetGlyphKerning(KMP_PTR(TTF_Font, font), (Uint32)previous_ch, (Uint32)ch, &kerning);
    if (out != NULL) {
        out[0] = kerning;
    }
    return ok;
}

int sdl_ttf_kmp_GetStringSize(int font, const char *text, int wrap_width, int *out)
{
    int w = 0, h = 0;
    int ok = wrap_width > 0
        ? TTF_GetStringSizeWrapped(KMP_PTR(TTF_Font, font), text, 0, wrap_width, &w, &h)
        : TTF_GetStringSize(KMP_PTR(TTF_Font, font), text, 0, &w, &h);
    if (out != NULL) {
        out[0] = w;
        out[1] = h;
    }
    return ok;
}

int sdl_ttf_kmp_MeasureString(int font, const char *text, int max_width, int *out)
{
    int width = 0;
    size_t length = 0;
    int ok = TTF_MeasureString(KMP_PTR(TTF_Font, font), text, 0, max_width, &width, &length);
    if (out != NULL) {
        out[0] = width;
        out[1] = (int)length;
    }
    return ok;
}

int sdl_ttf_kmp_AddFallbackFont(int font, int fallback)
{
    return TTF_AddFallbackFont(KMP_PTR(TTF_Font, font), KMP_PTR(TTF_Font, fallback));
}

void sdl_ttf_kmp_RemoveFallbackFont(int font, int fallback)
{
    TTF_RemoveFallbackFont(KMP_PTR(TTF_Font, font), KMP_PTR(TTF_Font, fallback));
}

void sdl_ttf_kmp_ClearFallbackFonts(int font)
{
    TTF_ClearFallbackFonts(KMP_PTR(TTF_Font, font));
}

/* =========================================================================
 * Text engines
 * ========================================================================= */

int sdl_ttf_kmp_CreateRendererTextEngine(int renderer)
{
    return KMP_HANDLE(TTF_CreateRendererTextEngine(KMP_PTR(SDL_Renderer, renderer)));
}

int sdl_ttf_kmp_CreateSurfaceTextEngine(void)
{
    return KMP_HANDLE(TTF_CreateSurfaceTextEngine());
}

int sdl_ttf_kmp_CreateGPUTextEngine(int device)
{
    return KMP_HANDLE(TTF_CreateGPUTextEngine(KMP_PTR(SDL_GPUDevice, device)));
}

void sdl_ttf_kmp_DestroyRendererTextEngine(int engine)
{
    TTF_DestroyRendererTextEngine(KMP_PTR(TTF_TextEngine, engine));
}

void sdl_ttf_kmp_DestroySurfaceTextEngine(int engine)
{
    TTF_DestroySurfaceTextEngine(KMP_PTR(TTF_TextEngine, engine));
}

void sdl_ttf_kmp_DestroyGPUTextEngine(int engine)
{
    TTF_DestroyGPUTextEngine(KMP_PTR(TTF_TextEngine, engine));
}

/* =========================================================================
 * Text objects
 * ========================================================================= */

int sdl_ttf_kmp_CreateText(int engine, int font, const char *text)
{
    return KMP_HANDLE(TTF_CreateText(
        KMP_PTR(TTF_TextEngine, engine), KMP_PTR(TTF_Font, font), text, 0));
}

void sdl_ttf_kmp_DestroyText(int text)
{
    TTF_DestroyText(KMP_PTR(TTF_Text, text));
}

const char *sdl_ttf_kmp_GetTextString(int text)
{
    TTF_Text *t = KMP_PTR(TTF_Text, text);
    return t != NULL ? t->text : NULL;
}

int sdl_ttf_kmp_GetTextNumLines(int text)
{
    TTF_Text *t = KMP_PTR(TTF_Text, text);
    return t != NULL ? t->num_lines : 0;
}

int sdl_ttf_kmp_SetTextColor(int text, int r, int g, int b, int a)
{
    return TTF_SetTextColor(KMP_PTR(TTF_Text, text), (Uint8)r, (Uint8)g, (Uint8)b, (Uint8)a);
}

int sdl_ttf_kmp_GetTextColor(int text, int *out)
{
    Uint8 r = 0, g = 0, b = 0, a = 0;
    int ok = TTF_GetTextColor(KMP_PTR(TTF_Text, text), &r, &g, &b, &a);
    if (out != NULL) {
        out[0] = r;
        out[1] = g;
        out[2] = b;
        out[3] = a;
    }
    return ok;
}

int sdl_ttf_kmp_SetTextPosition(int text, int x, int y)
{
    return TTF_SetTextPosition(KMP_PTR(TTF_Text, text), x, y);
}

int sdl_ttf_kmp_GetTextPosition(int text, int *out)
{
    int x = 0, y = 0;
    int ok = TTF_GetTextPosition(KMP_PTR(TTF_Text, text), &x, &y);
    if (out != NULL) {
        out[0] = x;
        out[1] = y;
    }
    return ok;
}

int sdl_ttf_kmp_SetTextWrapWidth(int text, int wrap_width)
{
    return TTF_SetTextWrapWidth(KMP_PTR(TTF_Text, text), wrap_width);
}

int sdl_ttf_kmp_GetTextWrapWidth(int text, int *out)
{
    int wrap_width = 0;
    int ok = TTF_GetTextWrapWidth(KMP_PTR(TTF_Text, text), &wrap_width);
    if (out != NULL) {
        out[0] = wrap_width;
    }
    return ok;
}

int sdl_ttf_kmp_TextWrapWhitespaceVisible(int text)
{
    return TTF_TextWrapWhitespaceVisible(KMP_PTR(TTF_Text, text));
}

int sdl_ttf_kmp_SetTextWrapWhitespaceVisible(int text, int visible)
{
    return TTF_SetTextWrapWhitespaceVisible(KMP_PTR(TTF_Text, text), visible != 0);
}

int sdl_ttf_kmp_GetTextSize(int text, int *out)
{
    int w = 0, h = 0;
    int ok = TTF_GetTextSize(KMP_PTR(TTF_Text, text), &w, &h);
    if (out != NULL) {
        out[0] = w;
        out[1] = h;
    }
    return ok;
}

int sdl_ttf_kmp_DrawRendererText(int text, float x, float y)
{
    return TTF_DrawRendererText(KMP_PTR(TTF_Text, text), x, y);
}

int sdl_ttf_kmp_DrawSurfaceText(int text, int x, int y, int surface)
{
    return TTF_DrawSurfaceText(KMP_PTR(TTF_Text, text), x, y, KMP_PTR(SDL_Surface, surface));
}

int sdl_ttf_kmp_SetTextString(int text, const char *string)
{
    return TTF_SetTextString(KMP_PTR(TTF_Text, text), string, 0);
}

int sdl_ttf_kmp_AppendTextString(int text, const char *string)
{
    return TTF_AppendTextString(KMP_PTR(TTF_Text, text), string, 0);
}

int sdl_ttf_kmp_InsertTextString(int text, int offset, const char *string)
{
    return TTF_InsertTextString(KMP_PTR(TTF_Text, text), offset, string, 0);
}

int sdl_ttf_kmp_DeleteTextString(int text, int offset, int length)
{
    return TTF_DeleteTextString(KMP_PTR(TTF_Text, text), offset, length);
}

static void kmp_store_substring(const TTF_SubString *sub, int *out)
{
    if (out == NULL) {
        return;
    }
    out[0] = (int)sub->flags;
    out[1] = sub->offset;
    out[2] = sub->length;
    out[3] = sub->line_index;
    out[4] = sub->rect.x;
    out[5] = sub->rect.y;
    out[6] = sub->rect.w;
    out[7] = sub->rect.h;
}

int sdl_ttf_kmp_GetTextSubString(int text, int offset, int *out)
{
    TTF_SubString sub;
    int ok = TTF_GetTextSubString(KMP_PTR(TTF_Text, text), offset, &sub);
    if (ok) {
        kmp_store_substring(&sub, out);
    }
    return ok;
}

int sdl_ttf_kmp_GetTextSubStringForLine(int text, int line, int *out)
{
    TTF_SubString sub;
    int ok = TTF_GetTextSubStringForLine(KMP_PTR(TTF_Text, text), line, &sub);
    if (ok) {
        kmp_store_substring(&sub, out);
    }
    return ok;
}

int sdl_ttf_kmp_GetTextSubStringForPoint(int text, int x, int y, int *out)
{
    TTF_SubString sub;
    int ok = TTF_GetTextSubStringForPoint(KMP_PTR(TTF_Text, text), x, y, &sub);
    if (ok) {
        kmp_store_substring(&sub, out);
    }
    return ok;
}

/* =========================================================================
 * Rendering
 * ========================================================================= */

int sdl_ttf_kmp_RenderTextSolid(int font, const char *text, int wrap_width, int r, int g, int b, int a)
{
    SDL_Color fg = kmp_color(r, g, b, a);
    SDL_Surface *surface = wrap_width > 0
        ? TTF_RenderText_Solid_Wrapped(KMP_PTR(TTF_Font, font), text, 0, fg, wrap_width)
        : TTF_RenderText_Solid(KMP_PTR(TTF_Font, font), text, 0, fg);
    return kmp_rgba32(surface);
}

int sdl_ttf_kmp_RenderTextShaded(int font, const char *text, int wrap_width, int r, int g, int b, int a, int br, int bg, int bb, int ba)
{
    SDL_Color fg = kmp_color(r, g, b, a);
    SDL_Color background = kmp_color(br, bg, bb, ba);
    SDL_Surface *surface = wrap_width > 0
        ? TTF_RenderText_Shaded_Wrapped(KMP_PTR(TTF_Font, font), text, 0, fg, background, wrap_width)
        : TTF_RenderText_Shaded(KMP_PTR(TTF_Font, font), text, 0, fg, background);
    return kmp_rgba32(surface);
}

int sdl_ttf_kmp_RenderTextBlended(int font, const char *text, int wrap_width, int r, int g, int b, int a)
{
    SDL_Color fg = kmp_color(r, g, b, a);
    SDL_Surface *surface = wrap_width > 0
        ? TTF_RenderText_Blended_Wrapped(KMP_PTR(TTF_Font, font), text, 0, fg, wrap_width)
        : TTF_RenderText_Blended(KMP_PTR(TTF_Font, font), text, 0, fg);
    return kmp_rgba32(surface);
}

int sdl_ttf_kmp_RenderTextLCD(int font, const char *text, int wrap_width, int r, int g, int b, int a, int br, int bg, int bb, int ba)
{
    SDL_Color fg = kmp_color(r, g, b, a);
    SDL_Color background = kmp_color(br, bg, bb, ba);
    SDL_Surface *surface = wrap_width > 0
        ? TTF_RenderText_LCD_Wrapped(KMP_PTR(TTF_Font, font), text, 0, fg, background, wrap_width)
        : TTF_RenderText_LCD(KMP_PTR(TTF_Font, font), text, 0, fg, background);
    return kmp_rgba32(surface);
}

int sdl_ttf_kmp_RenderGlyphSolid(int font, int ch, int r, int g, int b, int a)
{
    return kmp_rgba32(TTF_RenderGlyph_Solid(KMP_PTR(TTF_Font, font), (Uint32)ch, kmp_color(r, g, b, a)));
}

int sdl_ttf_kmp_RenderGlyphShaded(int font, int ch, int r, int g, int b, int a, int br, int bg, int bb, int ba)
{
    return kmp_rgba32(TTF_RenderGlyph_Shaded(
        KMP_PTR(TTF_Font, font), (Uint32)ch, kmp_color(r, g, b, a), kmp_color(br, bg, bb, ba)));
}

int sdl_ttf_kmp_RenderGlyphBlended(int font, int ch, int r, int g, int b, int a)
{
    return kmp_rgba32(TTF_RenderGlyph_Blended(KMP_PTR(TTF_Font, font), (Uint32)ch, kmp_color(r, g, b, a)));
}

int sdl_ttf_kmp_RenderGlyphLCD(int font, int ch, int r, int g, int b, int a, int br, int bg, int bb, int ba)
{
    return kmp_rgba32(TTF_RenderGlyph_LCD(
        KMP_PTR(TTF_Font, font), (Uint32)ch, kmp_color(r, g, b, a), kmp_color(br, bg, bb, ba)));
}

int sdl_ttf_kmp_GetGlyphImage(int font, int ch)
{
    return KMP_HANDLE(TTF_GetGlyphImage(KMP_PTR(TTF_Font, font), (Uint32)ch, NULL));
}
