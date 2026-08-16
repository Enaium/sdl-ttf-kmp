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

import cn.enaium.sdl.SDLColor
import cn.enaium.sdl.SDLGPUDevice
import cn.enaium.sdl.SDLGPUTexture
import cn.enaium.sdl.SDLIOStream
import cn.enaium.sdl.SDLPoint
import cn.enaium.sdl.SDLRect
import cn.enaium.sdl.SDLRenderer
import cn.enaium.sdl.SDLSurface

// =========================================================================
// Constants (values match SDL3_ttf's SDL_ttf.h)
// =========================================================================

/** Font style flags, see [SDLTTFFont.style]. */
object SDLTTFStyle {
    const val NORMAL = 0x00
    const val BOLD = 0x01
    const val ITALIC = 0x02
    const val UNDERLINE = 0x04
    const val STRIKETHROUGH = 0x08
}

/** Font hinting levels, see [SDLTTFFont.hinting]. */
object SDLTTFHinting {
    const val INVALID = -1
    const val NORMAL = 0
    const val LIGHT = 1
    const val MONO = 2
    const val NONE = 3
    const val LIGHT_SUBPIXEL = 4
}

/** Text direction (values match hb_direction_t). */
object SDLTTFDirection {
    const val INVALID = 0
    const val LTR = 4
    const val RTL = 5
    const val TTB = 6
    const val BTT = 7
}

/** Horizontal alignment for wrapped text. */
object SDLTTFHorizontalAlignment {
    const val INVALID = -1
    const val LEFT = 0
    const val CENTER = 1
    const val RIGHT = 2
}

/** Named font weight values, see [SDLTTFFont.weight]. */
object SDLTTFWeight {
    const val THIN = 100
    const val EXTRA_LIGHT = 200
    const val LIGHT = 300
    const val NORMAL = 400
    const val MEDIUM = 500
    const val SEMI_BOLD = 600
    const val BOLD = 700
    const val EXTRA_BOLD = 800
    const val BLACK = 900
    const val EXTRA_BLACK = 950
}

/** The type of data in a glyph image. */
object SDLTTFImageType {
    const val INVALID = 0
    const val ALPHA = 1
    const val COLOR = 2
    const val SDF = 3
}

/** Flags of a [SDLTTFSubString]. */
object SDLTTFSubStringFlags {
    const val DIRECTION_MASK = 0x000000FF
    const val TEXT_START = 0x00000100
    const val LINE_START = 0x00000200
    const val LINE_END = 0x00000400
    const val TEXT_END = 0x00000800
}

// =========================================================================
// Data types
// =========================================================================

/** A three-part library version (major, minor, patch). */
data class SDLTTFVersion(val major: Int, val minor: Int, val patch: Int)

/** The metrics of a glyph. */
data class SDLTTFGlyphMetrics(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val advance: Int,
)

/** The result of [SDLTTFFont.measureString]. */
data class SDLTTFMeasure(val width: Int, val length: Int)

/** A substring of a [SDLTTFText], see [SDLTTFSubStringFlags]. */
data class SDLTTFSubString(
    val flags: Int,
    val offset: Int,
    val length: Int,
    val lineIndex: Int,
    val rect: SDLRect,
)

/**
 * One draw sequence of a GPU-laid-out [SDLTTFText] (see
 * [SDLTTFText.getGPUDrawData]).
 *
 * [atlasTexture] is the texture atlas holding the glyphs (owned by the text
 * engine; do not close it), or null for a solid-fill sequence. [positions]
 * are vertex positions in pixels with positive Y upwards (the SDL_GPU
 * convention), [uvs] normalized texture coordinates; both are pairs
 * (x, y). [indices] reference the vertices.
 */
data class SDLTTFGPUAtlasDrawSequence(
    val atlasTexture: SDLGPUTexture?,
    val atlasTexturePtr: Long,
    val imageType: Int,
    val positions: FloatArray,
    val uvs: FloatArray,
    val indices: IntArray,
)

// =========================================================================
// Fonts
// =========================================================================

/**
 * A TTF_Font, created with [SDLTTF.openFont] or [SDLTTF.openFontIO].
 *
 * The font owns its resources; call [close] to release it. All methods should
 * be called on the thread that created the font.
 */
interface SDLTTFFont : AutoCloseable {

    /** The raw TTF_Font handle address, or 0 after [close]. */
    val ptr: Long

    /** The point size of the font. */
    var size: Float

    /** The font style, see [SDLTTFStyle]. */
    var style: Int

    /** The font outline in pixels (0 = no outline). */
    var outline: Int

    /** The font hinting, see [SDLTTFHinting]. */
    var hinting: Int

    /** Whether signed distance field rendering is enabled. */
    var SDF: Boolean

    /** The wrap alignment for wrapped text, see [SDLTTFHorizontalAlignment]. */
    var wrapAlignment: Int

    /** The line spacing in pixels. */
    var lineSkip: Int

    /** Whether kerning is enabled. */
    var kerning: Boolean

    /** The text shaping direction, see [SDLTTFDirection]. */
    var direction: Int

    /** Additional spacing in pixels between characters. */
    var charSpacing: Int

    /** The total font height in pixels. */
    val height: Int

    /** The offset from the baseline to the top of the font. */
    val ascent: Int

    /** The offset from the baseline to the bottom of the font (negative). */
    val descent: Int

    /** The font family name, or null. */
    val familyName: String?

    /** The font style name, or null. */
    val styleName: String?

    /** Whether the font is fixed-width (monospaced). */
    val fixedWidth: Boolean

    /** Whether the font is scalable. */
    val scalable: Boolean

    /** The font weight, see [SDLTTFWeight]. */
    val weight: Int

    /** Whether the font provides a glyph for the UNICODE codepoint [ch]. */
    fun hasGlyph(ch: Int): Boolean

    /** The metrics of the glyph for the codepoint [ch], or null on failure. */
    fun getGlyphMetrics(ch: Int): SDLTTFGlyphMetrics?

    /** The kerning in pixels between the glyphs of [previousCh] and [ch], or null on failure. */
    fun getGlyphKerning(previousCh: Int, ch: Int): Int?

    /** The size of the rendered [text], or null on failure. */
    fun getStringSize(text: String): SDLPoint?

    /** The size of the wrapped [text], or null on failure. */
    fun getStringSizeWrapped(text: String, wrapWidth: Int): SDLPoint?

    /** How much of [text] fits in [maxWidth] pixels, or null on failure. */
    fun measureString(text: String, maxWidth: Int): SDLTTFMeasure?

    /** Adds [fallback] for glyphs missing from this font. */
    fun addFallbackFont(fallback: SDLTTFFont): Boolean

    /** Removes a previously added [fallback] font. */
    fun removeFallbackFont(fallback: SDLTTFFont)

    /** Removes all fallback fonts. */
    fun clearFallbackFonts()

    /** Releases the font. */
    override fun close()
}

// =========================================================================
// Text engines / text objects
// =========================================================================

/**
 * A TTF_TextEngine, created with [SDLTTF.createRendererTextEngine] (draws on
 * an SDL renderer) or [SDLTTF.createSurfaceTextEngine] (draws on surfaces).
 */
interface SDLTTFTextEngine : AutoCloseable {

    /** The raw TTF_TextEngine handle address, or 0 after [close]. */
    val ptr: Long

    /** Releases the engine (all text created with it must be closed first). */
    override fun close()
}

/**
 * A TTF_Text, created with [SDLTTF.createText].
 *
 * The text re-lays out automatically when the font, string or wrap width
 * change. [SDLTTFTextEngine] determines where [draw] renders: a renderer
 * engine draws directly on its SDL_Renderer, a surface engine needs
 * [drawOnSurface].
 */
interface SDLTTFText : AutoCloseable {

    /** The raw TTF_Text handle address, or 0 after [close]. */
    val ptr: Long

    /** The current UTF-8 string of this text object. */
    val text: String?

    /** The number of lines, 0 if empty. */
    val numLines: Int

    /** The text color (default white). */
    var color: SDLColor

    /** The position offset in pixels, or null when unset. */
    var position: SDLPoint?

    /** The maximum wrap width in pixels (0 = wrap on newlines only). */
    var wrapWidth: Int

    /** Whether whitespace is visible when wrapping. */
    var wrapWhitespaceVisible: Boolean

    /** The size of the text in pixels, or null on failure. */
    val size: SDLPoint?

    /** Draws the text at ([x], [y]) with the engine's SDL_Renderer. */
    fun draw(x: Float, y: Float): Boolean

    /** Draws the text at ([x], [y]) on [surface] (surface text engine only). */
    fun drawOnSurface(x: Int, y: Int, surface: SDLSurface): Boolean

    /** Replaces the text string. */
    fun setText(text: String): Boolean

    /** Appends [text] to the current string. */
    fun append(text: String): Boolean

    /** Inserts [text] at the byte [offset]. */
    fun insert(offset: Int, text: String): Boolean

    /** Deletes [length] bytes at the byte [offset] (-1 = to the end). */
    fun delete(offset: Int, length: Int): Boolean

    /** The substring at the byte [offset], or null on failure. */
    fun subStringAt(offset: Int): SDLTTFSubString?

    /** The substring of the given [line], or null on failure. */
    fun subStringForLine(line: Int): SDLTTFSubString?

    /** The substring closest to ([x], [y]) relative to the text, or null. */
    fun subStringForPoint(x: Int, y: Int): SDLTTFSubString?

    /**
     * The draw sequences of this text for the GPU API (the text must have
     * been created with a GPU text engine), or null on failure. Vertices use
     * pixel coordinates with positive Y upwards.
     */
    fun getGPUDrawData(): List<SDLTTFGPUAtlasDrawSequence>?

    /** Releases the text object. */
    override fun close()
}

// =========================================================================
// The SDL_ttf API
// =========================================================================

/**
 * Kotlin Multiplatform bindings for SDL_ttf 3.x, built on top of sdl-kmp.
 *
 *  - fonts: [openFont], [openFontIO], [copyFont]
 *  - rendering: [renderTextBlended] and friends return [SDLSurface]s that plug
 *    directly into sdl-kmp's renderer API (e.g.
 *    `renderer.createTextureFromSurface`)
 *  - text engines: [createRendererTextEngine] draws text directly on an
 *    sdl-kmp [SDLRenderer] via [SDLTTFText.draw]; [createSurfaceTextEngine]
 *    draws on [SDLSurface]s via [SDLTTFText.drawOnSurface]
 *  - metrics: [SDLTTFFont.getStringSize], [SDLTTFFont.measureString], glyph
 *    metrics and kerning
 *
 * On the JVM the bindings delegate to libsdl_ttf_jni, a self-contained JNI
 * shared library whose SDL3 symbols are resolved at runtime from sdl-kmp's
 * libsdl_jni; on native platforms they delegate to the SDL_ttf static library
 * embedded in the published klib (see the ttf.def cinterop file).
 */
expect object SDLTTF {

    /**
     * Initializes SDL_ttf. Must succeed before any other function is called.
     * Safe to call multiple times; pair each success with [quit].
     */
    fun init(): Boolean

    /** Deinitializes SDL_ttf (pairs with each successful [init]). */
    fun quit()

    /** The number of outstanding [init] calls, or 0 when not initialized. */
    fun wasInit(): Int

    /** The version of the underlying SDL_ttf library. */
    fun version(): Int

    /** The version of the FreeType library in use. */
    fun getFreeTypeVersion(): SDLTTFVersion

    /** The version of the HarfBuzz library in use (0.0.0 when unavailable). */
    fun getHarfBuzzVersion(): SDLTTFVersion

    /** The last error set by SDL_ttf, or null if there is none. */
    fun error(): String?

    /** Clears the last error set by SDL_ttf. */
    fun clearError()

    /**
     * Opens the font at [path] with the given [pointSize] and returns it, or
     * throws an exception describing the SDL error.
     */
    fun openFont(path: String, pointSize: Float): SDLTTFFont

    /**
     * Opens the font from the SDL [stream] with the given [pointSize].
     * When [closeIO] is true, [SDLIOStream.close] is called when the font
     * closes. The stream must stay open for the font's lifetime otherwise.
     */
    fun openFontIO(stream: SDLIOStream, closeIO: Boolean = false, pointSize: Float): SDLTTFFont

    /**
     * Creates a copy of [existingFont] sharing the font file but with its own
     * size and style, or throws an exception describing the SDL error.
     */
    fun copyFont(existingFont: SDLTTFFont): SDLTTFFont

    /**
     * Creates a text engine that draws on the SDL renderer [renderer], or
     * throws an exception describing the SDL error.
     */
    fun createRendererTextEngine(renderer: SDLRenderer): SDLTTFTextEngine

    /** Creates a text engine that draws on SDL surfaces, or throws. */
    fun createSurfaceTextEngine(): SDLTTFTextEngine

    /**
     * Creates a text engine that lays text out for the SDL GPU API (the
     * draw data is obtained with [SDLTTFText.getGPUDrawData] and rendered
     * by the application), or throws an exception describing the SDL error.
     */
    fun createGPUTextEngine(device: SDLGPUDevice): SDLTTFTextEngine

    /**
     * Creates a text object; [engine] and [font] may be null and set later
     * via [SDLTTFTextEngine] assignment APIs.
     */
    fun createText(engine: SDLTTFTextEngine?, font: SDLTTFFont?, text: String): SDLTTFText

    /** Renders [text] at fast quality to a new 8-bit palettized surface. */
    fun renderTextSolid(font: SDLTTFFont, text: String, fg: SDLColor, wrapWidth: Int = 0): SDLSurface?

    /** Renders [text] at high quality to a new 8-bit surface with [bg]. */
    fun renderTextShaded(
        font: SDLTTFFont,
        text: String,
        fg: SDLColor,
        bg: SDLColor,
        wrapWidth: Int = 0,
    ): SDLSurface?

    /** Renders [text] at high quality to a new 32-bit ARGB surface. */
    fun renderTextBlended(font: SDLTTFFont, text: String, fg: SDLColor, wrapWidth: Int = 0): SDLSurface?

    /** Renders [text] at LCD subpixel quality to a new 32-bit ARGB surface. */
    fun renderTextLCD(
        font: SDLTTFFont,
        text: String,
        fg: SDLColor,
        bg: SDLColor,
        wrapWidth: Int = 0,
    ): SDLSurface?

    /** Renders the glyph [ch] at fast quality to a new 8-bit surface. */
    fun renderGlyphSolid(font: SDLTTFFont, ch: Int, fg: SDLColor): SDLSurface?

    /** Renders the glyph [ch] at high quality to a new 8-bit surface with [bg]. */
    fun renderGlyphShaded(font: SDLTTFFont, ch: Int, fg: SDLColor, bg: SDLColor): SDLSurface?

    /** Renders the glyph [ch] at high quality to a new 32-bit ARGB surface. */
    fun renderGlyphBlended(font: SDLTTFFont, ch: Int, fg: SDLColor): SDLSurface?

    /** Renders the glyph [ch] at LCD quality to a new 32-bit ARGB surface. */
    fun renderGlyphLCD(font: SDLTTFFont, ch: Int, fg: SDLColor, bg: SDLColor): SDLSurface?

    /** The pixel image of the glyph [ch] as a surface, or null on failure. */
    fun getGlyphImage(font: SDLTTFFont, ch: Int): SDLSurface?
}
