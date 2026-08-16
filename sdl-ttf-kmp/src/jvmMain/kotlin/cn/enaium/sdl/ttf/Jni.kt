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

package cn.enaium.sdl.ttf

/**
 * JNI bridge for the JVM target.
 *
 * Every `external fun` maps 1:1 to a `Java_cn.enaium.sdl.ttf_Jni_<name>`
 * function in jni/jni_ttf.cpp (see the naming convention in sdl-kmp's
 * jni_bridge.h). All members are public (no `internal` modifier) so their JVM
 * names are not mangled by the Kotlin compiler.
 *
 * The underlying libsdl_ttf_jni shared library references the SDL3 symbols of
 * libsdl_jni (from the sdl-kmp project), so sdl-kmp's JNI library is loaded
 * first in the init block.
 */
internal object Jni {

    init {
        // Force sdl-kmp's Jni object to initialize, loading libsdl_jni and
        // registering its SDL3 symbols in the process namespace before
        // libsdl_ttf_jni is dlopen()ed.
        cn.enaium.sdl.SDL.error()
        TtfNativeLoader.load()
    }

    // =========================================================================
    // Core
    // =========================================================================

    external fun init(): Boolean
    external fun quit()
    external fun wasInit(): Int
    external fun version(): Int
    external fun getFreeTypeVersion(): IntArray
    external fun getHarfBuzzVersion(): IntArray
    external fun getError(): String?
    external fun setError(message: String): Boolean

    // =========================================================================
    // Fonts
    // =========================================================================

    external fun openFont(path: String, pointSize: Float): Long
    external fun openFontIO(stream: Long, closeIO: Boolean, pointSize: Float): Long
    external fun copyFont(font: Long): Long
    external fun closeFont(font: Long)
    external fun setFontSize(font: Long, pointSize: Float): Boolean
    external fun setFontSizeDPI(font: Long, pointSize: Float, hdpi: Int, vdpi: Int): Boolean
    external fun getFontSize(font: Long): Float
    external fun getFontDPI(font: Long): IntArray?
    external fun setFontStyle(font: Long, style: Int)
    external fun getFontStyle(font: Long): Int
    external fun setFontOutline(font: Long, outline: Int): Boolean
    external fun getFontOutline(font: Long): Int
    external fun setFontHinting(font: Long, hinting: Int)
    external fun getFontHinting(font: Long): Int
    external fun setFontSDF(font: Long, enabled: Boolean): Boolean
    external fun getFontSDF(font: Long): Boolean
    external fun setFontWrapAlignment(font: Long, align: Int)
    external fun getFontWrapAlignment(font: Long): Int
    external fun setFontLineSkip(font: Long, lineskip: Int)
    external fun getFontLineSkip(font: Long): Int
    external fun getFontHeight(font: Long): Int
    external fun getFontAscent(font: Long): Int
    external fun getFontDescent(font: Long): Int
    external fun setFontKerning(font: Long, enabled: Boolean)
    external fun getFontKerning(font: Long): Boolean
    external fun fontIsFixedWidth(font: Long): Boolean
    external fun fontIsScalable(font: Long): Boolean
    external fun getFontFamilyName(font: Long): String?
    external fun getFontStyleName(font: Long): String?
    external fun setFontDirection(font: Long, direction: Int): Boolean
    external fun getFontDirection(font: Long): Int
    external fun setFontCharSpacing(font: Long, spacing: Int): Boolean
    external fun getFontCharSpacing(font: Long): Int
    external fun getFontWeight(font: Long): Int
    external fun addFallbackFont(font: Long, fallback: Long): Boolean
    external fun removeFallbackFont(font: Long, fallback: Long)
    external fun clearFallbackFonts(font: Long)

    // =========================================================================
    // Glyphs / metrics
    // =========================================================================

    external fun fontHasGlyph(font: Long, ch: Int): Boolean
    external fun getGlyphMetrics(font: Long, ch: Int): IntArray?
    external fun getGlyphKerning(font: Long, previousCh: Int, ch: Int): IntArray?
    external fun getStringSize(font: Long, text: String, length: Int): IntArray?
    external fun getStringSizeWrapped(font: Long, text: String, length: Int, wrapWidth: Int): IntArray?
    external fun measureString(font: Long, text: String, length: Int, maxWidth: Int): IntArray?

    // =========================================================================
    // Rendering to surfaces
    // =========================================================================

    external fun renderTextSolid(font: Long, text: String, length: Int, r: Int, g: Int, b: Int, a: Int): Long
    external fun renderTextSolidWrapped(font: Long, text: String, length: Int, r: Int, g: Int, b: Int, a: Int, wrapWidth: Int): Long
    external fun renderTextShaded(font: Long, text: String, length: Int, r: Int, g: Int, b: Int, a: Int, br: Int, bg: Int, bb: Int, ba: Int): Long
    external fun renderTextShadedWrapped(font: Long, text: String, length: Int, r: Int, g: Int, b: Int, a: Int, br: Int, bg: Int, bb: Int, ba: Int, wrapWidth: Int): Long
    external fun renderTextBlended(font: Long, text: String, length: Int, r: Int, g: Int, b: Int, a: Int): Long
    external fun renderTextBlendedWrapped(font: Long, text: String, length: Int, r: Int, g: Int, b: Int, a: Int, wrapWidth: Int): Long
    external fun renderTextLCD(font: Long, text: String, length: Int, r: Int, g: Int, b: Int, a: Int, br: Int, bg: Int, bb: Int, ba: Int): Long
    external fun renderTextLCDWrapped(font: Long, text: String, length: Int, r: Int, g: Int, b: Int, a: Int, br: Int, bg: Int, bb: Int, ba: Int, wrapWidth: Int): Long
    external fun renderGlyphSolid(font: Long, ch: Int, r: Int, g: Int, b: Int, a: Int): Long
    external fun renderGlyphShaded(font: Long, ch: Int, r: Int, g: Int, b: Int, a: Int, br: Int, bg: Int, bb: Int, ba: Int): Long
    external fun renderGlyphBlended(font: Long, ch: Int, r: Int, g: Int, b: Int, a: Int): Long
    external fun renderGlyphLCD(font: Long, ch: Int, r: Int, g: Int, b: Int, a: Int, br: Int, bg: Int, bb: Int, ba: Int): Long
    external fun getGlyphImage(font: Long, ch: Int): Long
    external fun getGlyphImageForIndex(font: Long, glyphIndex: Int): Long

    // =========================================================================
    // Surfaces (wrap SDL_ttf-rendered SDL_Surfaces into SDLSurface)
    // =========================================================================

    external fun surfaceWidth(surface: Long): Int
    external fun surfaceHeight(surface: Long): Int
    external fun surfaceFormat(surface: Long): Int
    external fun surfacePitch(surface: Long): Int
    external fun surfaceColorspace(surface: Long): Int
    external fun surfacePixels(surface: Long): ByteArray?
    external fun lockSurface(surface: Long): Boolean
    external fun unlockSurface(surface: Long)
    external fun surfaceFillRect(surface: Long, rect: IntArray?, r: Int, g: Int, b: Int, a: Int): Boolean
    external fun surfaceFillRects(surface: Long, rects: IntArray, r: Int, g: Int, b: Int, a: Int): Boolean
    external fun surfaceBlit(src: Long, srcRect: IntArray?, dst: Long, dstRect: IntArray?): Boolean
    external fun surfaceBlitScaled(src: Long, srcRect: IntArray?, dst: Long, dstRect: IntArray?, scaleMode: Int): Boolean
    external fun surfaceSaveBMP(surface: Long, path: String): Boolean
    external fun convertSurface(surface: Long, format: Int): Long
    external fun destroySurface(surface: Long)

    // =========================================================================
    // Text engines
    // =========================================================================

    external fun createRendererTextEngine(renderer: Long): Long
    external fun createSurfaceTextEngine(): Long
    external fun destroyRendererTextEngine(engine: Long)
    external fun destroySurfaceTextEngine(engine: Long)

    // =========================================================================
    // Text objects
    // =========================================================================

    external fun createText(engine: Long, font: Long, text: String, length: Int): Long
    external fun destroyText(text: Long)
    external fun setTextEngine(text: Long, engine: Long): Boolean
    external fun setTextFont(text: Long, font: Long): Boolean
    external fun setTextDirection(text: Long, direction: Int): Boolean
    external fun getTextDirection(text: Long): Int
    external fun setTextColor(text: Long, r: Int, g: Int, b: Int, a: Int): Boolean
    external fun getTextColor(text: Long): IntArray?
    external fun setTextPosition(text: Long, x: Int, y: Int): Boolean
    external fun getTextPosition(text: Long): IntArray?
    external fun setTextWrapWidth(text: Long, wrapWidth: Int): Boolean
    external fun getTextWrapWidth(text: Long): Int
    external fun setTextWrapWhitespaceVisible(text: Long, visible: Boolean): Boolean
    external fun textWrapWhitespaceVisible(text: Long): Boolean
    external fun setTextString(text: Long, string: String, length: Int): Boolean
    external fun appendTextString(text: Long, string: String, length: Int): Boolean
    external fun insertTextString(text: Long, offset: Int, string: String, length: Int): Boolean
    external fun deleteTextString(text: Long, offset: Int, length: Int): Boolean
    external fun getTextString(text: Long): String?
    external fun getTextNumLines(text: Long): Int
    external fun getTextSize(text: Long): IntArray?
    external fun drawRendererText(text: Long, x: Float, y: Float): Boolean
    external fun drawSurfaceText(text: Long, x: Int, y: Int, surface: Long): Boolean
    external fun updateText(text: Long): Boolean
    external fun getTextSubString(text: Long, offset: Int): IntArray?
    external fun getTextSubStringForLine(text: Long, line: Int): IntArray?
    external fun getTextSubStringForPoint(text: Long, x: Int, y: Int): IntArray?
}
