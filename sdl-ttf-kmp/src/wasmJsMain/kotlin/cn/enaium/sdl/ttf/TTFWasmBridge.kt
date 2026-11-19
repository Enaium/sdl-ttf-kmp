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

@file:OptIn(ExperimentalJsExport::class)

package cn.enaium.sdl.ttf

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Int32Array
import kotlin.js.ExperimentalJsExport

/*
 * Declarations for the JS glue (ttf_kmp_glue.js, shipped in the
 * @enaium/sdl-ttf-kmp npm package) that wraps the combined
 * SDL3 + SDL_ttf Emscripten module. The host page initializes the module
 * with initSdlTtfKmp() BEFORE the Kotlin module runs, so these globals
 * always exist.
 *
 * The sdl_kmp_* declarations are the functions installed on the same module
 * by sdl-kmp's glue; sdl-ttf-kmp redeclares the few it needs because the
 * sdl-kmp wasmJs declarations are internal to that module.
 *
 * Result-buffer convention: functions with C out-parameters write their
 * results into the global sdlKmpResultI32 array (fixed slots), which the
 * Kotlin caller reads after the call.
 */

internal external fun sdlKmpResultI32Get(index: Int): Int

/** Live read-only view over the Emscripten heap starting at [ptr]. */
internal external fun sdlKmpHeapBytes(ptr: Int, len: Int): Int8Array

/** Copies [bytes] into the Emscripten heap starting at [ptr]. */
internal external fun sdlKmpSetHeapBytes(ptr: Int, bytes: Int8Array)

// =========================================================================
// SDL3 surface API (installed by sdl-kmp's glue on the same module)
// =========================================================================

internal external fun sdl_kmp_GetSurfaceWidth(surface: Int): Int
internal external fun sdl_kmp_GetSurfaceHeight(surface: Int): Int
internal external fun sdl_kmp_GetSurfaceFormat(surface: Int): Int
internal external fun sdl_kmp_GetSurfaceColorspace(surface: Int): Int
internal external fun sdl_kmp_GetSurfacePitch(surface: Int): Int
internal external fun sdl_kmp_GetSurfacePixels(surface: Int): Int
internal external fun sdl_kmp_LockSurface(surface: Int): Int
internal external fun sdl_kmp_UnlockSurface(surface: Int)
internal external fun sdl_kmp_FillSurfaceRect(surface: Int, hasRect: Int, x: Int, y: Int, width: Int, height: Int, color: Int): Int
internal external fun sdl_kmp_FillSurfaceRects(surface: Int, rects: Int32Array, count: Int, color: Int): Int
internal external fun sdl_kmp_BlitSurface(src: Int, hasSrc: Int, sx: Int, sy: Int, sw: Int, sh: Int, dst: Int, hasDst: Int, dx: Int, dy: Int, dw: Int, dh: Int): Int
internal external fun sdl_kmp_BlitSurfaceScaled(src: Int, hasSrc: Int, sx: Int, sy: Int, sw: Int, sh: Int, dst: Int, hasDst: Int, dx: Int, dy: Int, dw: Int, dh: Int, scaleMode: Int): Int
internal external fun sdl_kmp_SaveBMP(surface: Int, path: String): Int
internal external fun sdl_kmp_ConvertSurface(surface: Int, format: Int): Int
internal external fun sdl_kmp_DestroySurface(surface: Int)

// =========================================================================
// SDL_ttf library
// =========================================================================

internal external fun sdl_ttf_kmp_Init(): Int
internal external fun sdl_ttf_kmp_Quit()
internal external fun sdl_ttf_kmp_WasInit(): Int
internal external fun sdl_ttf_kmp_Version(): Int

/** Writes major/minor/patch to result slots 0..2. */
internal external fun sdl_ttf_kmp_GetFreeTypeVersion()

/** Writes major/minor/patch to result slots 0..2. */
internal external fun sdl_ttf_kmp_GetHarfBuzzVersion()

// =========================================================================
// Fonts
// =========================================================================

internal external fun sdl_ttf_kmp_OpenFont(path: String, pointSize: Float): Int
internal external fun sdl_ttf_kmp_OpenFontIO(stream: Int, closeIO: Int, pointSize: Float): Int
internal external fun sdl_ttf_kmp_CopyFont(font: Int): Int
internal external fun sdl_ttf_kmp_CloseFont(font: Int)
internal external fun sdl_ttf_kmp_GetFontSize(font: Int): Float
internal external fun sdl_ttf_kmp_SetFontSize(font: Int, pointSize: Float): Int
internal external fun sdl_ttf_kmp_GetFontStyle(font: Int): Int
internal external fun sdl_ttf_kmp_SetFontStyle(font: Int, style: Int)
internal external fun sdl_ttf_kmp_GetFontOutline(font: Int): Int
internal external fun sdl_ttf_kmp_SetFontOutline(font: Int, outline: Int)
internal external fun sdl_ttf_kmp_GetFontHinting(font: Int): Int
internal external fun sdl_ttf_kmp_SetFontHinting(font: Int, hinting: Int)
internal external fun sdl_ttf_kmp_GetFontSDF(font: Int): Int
internal external fun sdl_ttf_kmp_SetFontSDF(font: Int, enabled: Int): Int
internal external fun sdl_ttf_kmp_GetFontWrapAlignment(font: Int): Int
internal external fun sdl_ttf_kmp_SetFontWrapAlignment(font: Int, align: Int)
internal external fun sdl_ttf_kmp_GetFontLineSkip(font: Int): Int
internal external fun sdl_ttf_kmp_SetFontLineSkip(font: Int, lineSkip: Int)
internal external fun sdl_ttf_kmp_GetFontKerning(font: Int): Int
internal external fun sdl_ttf_kmp_SetFontKerning(font: Int, enabled: Int)
internal external fun sdl_ttf_kmp_GetFontDirection(font: Int): Int
internal external fun sdl_ttf_kmp_SetFontDirection(font: Int, direction: Int)
internal external fun sdl_ttf_kmp_GetFontHeight(font: Int): Int
internal external fun sdl_ttf_kmp_GetFontAscent(font: Int): Int
internal external fun sdl_ttf_kmp_GetFontDescent(font: Int): Int
internal external fun sdl_ttf_kmp_GetFontFamilyName(font: Int): String?
internal external fun sdl_ttf_kmp_GetFontStyleName(font: Int): String?
internal external fun sdl_ttf_kmp_FontIsFixedWidth(font: Int): Int
internal external fun sdl_ttf_kmp_FontIsScalable(font: Int): Int
internal external fun sdl_ttf_kmp_GetFontWeight(font: Int): Int
internal external fun sdl_ttf_kmp_FontHasGlyph(font: Int, ch: Int): Int

/** Writes minX/maxX/minY/maxY/advance to slots 0..4, success to slot 5. */
internal external fun sdl_ttf_kmp_GetGlyphMetrics(font: Int, ch: Int): Int

/** Writes kerning to slot 0, success to slot 1. */
internal external fun sdl_ttf_kmp_GetGlyphKerning(font: Int, previousCh: Int, ch: Int): Int

/** Writes width/height to slots 0..1, success to slot 2. */
internal external fun sdl_ttf_kmp_GetStringSize(font: Int, text: String, wrapWidth: Int): Int

/** Writes measured width/length to slots 0..1, success to slot 2. */
internal external fun sdl_ttf_kmp_MeasureString(font: Int, text: String, maxWidth: Int): Int

internal external fun sdl_ttf_kmp_AddFallbackFont(font: Int, fallback: Int): Int
internal external fun sdl_ttf_kmp_RemoveFallbackFont(font: Int, fallback: Int)
internal external fun sdl_ttf_kmp_ClearFallbackFonts(font: Int)

// =========================================================================
// Text engines
// =========================================================================

internal external fun sdl_ttf_kmp_CreateRendererTextEngine(renderer: Int): Int
internal external fun sdl_ttf_kmp_CreateSurfaceTextEngine(): Int
internal external fun sdl_ttf_kmp_CreateGPUTextEngine(device: Int): Int
internal external fun sdl_ttf_kmp_DestroyRendererTextEngine(engine: Int)
internal external fun sdl_ttf_kmp_DestroySurfaceTextEngine(engine: Int)
internal external fun sdl_ttf_kmp_DestroyGPUTextEngine(engine: Int)

// =========================================================================
// Text objects
// =========================================================================

internal external fun sdl_ttf_kmp_CreateText(engine: Int, font: Int, text: String): Int
internal external fun sdl_ttf_kmp_DestroyText(text: Int)
internal external fun sdl_ttf_kmp_GetTextString(text: Int): String?
internal external fun sdl_ttf_kmp_GetTextNumLines(text: Int): Int
internal external fun sdl_ttf_kmp_SetTextColor(text: Int, r: Int, g: Int, b: Int, a: Int): Int

/** Writes r/g/b/a to slots 0..3, success to slot 4. */
internal external fun sdl_ttf_kmp_GetTextColor(text: Int): Int

internal external fun sdl_ttf_kmp_SetTextPosition(text: Int, x: Int, y: Int): Int

/** Writes x/y to slots 0..1, success to slot 2. */
internal external fun sdl_ttf_kmp_GetTextPosition(text: Int): Int

internal external fun sdl_ttf_kmp_SetTextWrapWidth(text: Int, wrapWidth: Int): Int

/** Writes the wrap width to slot 0, success to slot 1. */
internal external fun sdl_ttf_kmp_GetTextWrapWidth(text: Int): Int

internal external fun sdl_ttf_kmp_TextWrapWhitespaceVisible(text: Int): Int
internal external fun sdl_ttf_kmp_SetTextWrapWhitespaceVisible(text: Int, visible: Int): Int

/** Writes width/height to slots 0..1, success to slot 2. */
internal external fun sdl_ttf_kmp_GetTextSize(text: Int): Int

internal external fun sdl_ttf_kmp_DrawRendererText(text: Int, x: Float, y: Float): Int
internal external fun sdl_ttf_kmp_DrawSurfaceText(text: Int, x: Int, y: Int, surface: Int): Int
internal external fun sdl_ttf_kmp_SetTextString(text: Int, string: String): Int
internal external fun sdl_ttf_kmp_AppendTextString(text: Int, string: String): Int
internal external fun sdl_ttf_kmp_InsertTextString(text: Int, offset: Int, string: String): Int
internal external fun sdl_ttf_kmp_DeleteTextString(text: Int, offset: Int, length: Int): Int

/** Writes flags/offset/length/lineIndex/rect.x/rect.y/rect.w/rect.h to slots 0..7, success to slot 8. */
internal external fun sdl_ttf_kmp_GetTextSubString(text: Int, offset: Int): Int

/** Same result layout as [sdl_ttf_kmp_GetTextSubString]. */
internal external fun sdl_ttf_kmp_GetTextSubStringForLine(text: Int, line: Int): Int

/** Same result layout as [sdl_ttf_kmp_GetTextSubString]. */
internal external fun sdl_ttf_kmp_GetTextSubStringForPoint(text: Int, x: Int, y: Int): Int

// =========================================================================
// Rendering
// =========================================================================

internal external fun sdl_ttf_kmp_RenderTextSolid(font: Int, text: String, wrapWidth: Int, r: Int, g: Int, b: Int, a: Int): Int
internal external fun sdl_ttf_kmp_RenderTextShaded(font: Int, text: String, wrapWidth: Int, fr: Int, fg: Int, fb: Int, fa: Int, br: Int, bg: Int, bb: Int, ba: Int): Int
internal external fun sdl_ttf_kmp_RenderTextBlended(font: Int, text: String, wrapWidth: Int, r: Int, g: Int, b: Int, a: Int): Int
internal external fun sdl_ttf_kmp_RenderTextLCD(font: Int, text: String, wrapWidth: Int, fr: Int, fg: Int, fb: Int, fa: Int, br: Int, bg: Int, bb: Int, ba: Int): Int
internal external fun sdl_ttf_kmp_RenderGlyphSolid(font: Int, ch: Int, r: Int, g: Int, b: Int, a: Int): Int
internal external fun sdl_ttf_kmp_RenderGlyphShaded(font: Int, ch: Int, fr: Int, fg: Int, fb: Int, fa: Int, br: Int, bg: Int, bb: Int, ba: Int): Int
internal external fun sdl_ttf_kmp_RenderGlyphBlended(font: Int, ch: Int, r: Int, g: Int, b: Int, a: Int): Int
internal external fun sdl_ttf_kmp_RenderGlyphLCD(font: Int, ch: Int, fr: Int, fg: Int, fb: Int, fa: Int, br: Int, bg: Int, bb: Int, ba: Int): Int
internal external fun sdl_ttf_kmp_GetGlyphImage(font: Int, ch: Int): Int
