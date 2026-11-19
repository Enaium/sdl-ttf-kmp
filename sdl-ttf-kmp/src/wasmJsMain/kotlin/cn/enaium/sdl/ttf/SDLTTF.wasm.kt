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

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLColor
import cn.enaium.sdl.SDLGPUDevice
import cn.enaium.sdl.SDLIOStream
import cn.enaium.sdl.SDLPoint
import cn.enaium.sdl.SDLRect
import cn.enaium.sdl.SDLRenderer
import cn.enaium.sdl.SDLSurface

private fun r32(index: Int): Int = sdlKmpResultI32Get(index)

internal class WasmTTFFont(handle: Int) : SDLTTFFont {

    internal var font: Int = handle

    override val ptr: Long get() = if (font == 0) 0L else font.toLong()

    internal fun check(): Int =
        font.also { if (it == 0) throw IllegalStateException("SDL_ttf font is closed") }

    override var size: Float
        get() = sdl_ttf_kmp_GetFontSize(check())
        set(value) { sdl_ttf_kmp_SetFontSize(check(), value) }

    override var style: Int
        get() = sdl_ttf_kmp_GetFontStyle(check())
        set(value) { sdl_ttf_kmp_SetFontStyle(check(), value) }

    override var outline: Int
        get() = sdl_ttf_kmp_GetFontOutline(check())
        set(value) { sdl_ttf_kmp_SetFontOutline(check(), value) }

    override var hinting: Int
        get() = sdl_ttf_kmp_GetFontHinting(check())
        set(value) { sdl_ttf_kmp_SetFontHinting(check(), value) }

    override var SDF: Boolean
        get() = sdl_ttf_kmp_GetFontSDF(check()) != 0
        set(value) { sdl_ttf_kmp_SetFontSDF(check(), if (value) 1 else 0) }

    override var wrapAlignment: Int
        get() = sdl_ttf_kmp_GetFontWrapAlignment(check())
        set(value) { sdl_ttf_kmp_SetFontWrapAlignment(check(), value) }

    override var lineSkip: Int
        get() = sdl_ttf_kmp_GetFontLineSkip(check())
        set(value) { sdl_ttf_kmp_SetFontLineSkip(check(), value) }

    override var kerning: Boolean
        get() = sdl_ttf_kmp_GetFontKerning(check()) != 0
        set(value) { sdl_ttf_kmp_SetFontKerning(check(), if (value) 1 else 0) }

    override var direction: Int
        get() = sdl_ttf_kmp_GetFontDirection(check())
        set(value) { sdl_ttf_kmp_SetFontDirection(check(), value) }

    override val height: Int get() = sdl_ttf_kmp_GetFontHeight(check())
    override val ascent: Int get() = sdl_ttf_kmp_GetFontAscent(check())
    override val descent: Int get() = sdl_ttf_kmp_GetFontDescent(check())
    override val familyName: String? get() = sdl_ttf_kmp_GetFontFamilyName(check())
    override val styleName: String? get() = sdl_ttf_kmp_GetFontStyleName(check())
    override val fixedWidth: Boolean get() = sdl_ttf_kmp_FontIsFixedWidth(check()) != 0
    override val scalable: Boolean get() = sdl_ttf_kmp_FontIsScalable(check()) != 0
    override val weight: Int get() = sdl_ttf_kmp_GetFontWeight(check())

    override fun hasGlyph(ch: Int): Boolean = sdl_ttf_kmp_FontHasGlyph(check(), ch) != 0

    override fun getGlyphMetrics(ch: Int): SDLTTFGlyphMetrics? {
        if (sdl_ttf_kmp_GetGlyphMetrics(check(), ch) == 0) return null
        return SDLTTFGlyphMetrics(
            minX = r32(0),
            maxX = r32(1),
            minY = r32(2),
            maxY = r32(3),
            advance = r32(4),
        )
    }

    override fun getGlyphKerning(previousCh: Int, ch: Int): Int? {
        if (sdl_ttf_kmp_GetGlyphKerning(check(), previousCh, ch) == 0) return null
        return r32(0)
    }

    override fun getStringSize(text: String): SDLPoint? {
        if (sdl_ttf_kmp_GetStringSize(check(), text, 0) == 0) return null
        return SDLPoint(r32(0), r32(1))
    }

    override fun getStringSizeWrapped(text: String, wrapWidth: Int): SDLPoint? {
        if (sdl_ttf_kmp_GetStringSize(check(), text, wrapWidth) == 0) return null
        return SDLPoint(r32(0), r32(1))
    }

    override fun measureString(text: String, maxWidth: Int): SDLTTFMeasure? {
        if (sdl_ttf_kmp_MeasureString(check(), text, maxWidth) == 0) return null
        return SDLTTFMeasure(r32(0), r32(1))
    }

    override fun addFallbackFont(fallback: SDLTTFFont): Boolean {
        val fallbackPtr = (fallback as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("fallback is not a wasm SDL_ttf font")
        return sdl_ttf_kmp_AddFallbackFont(check(), fallbackPtr) != 0
    }

    override fun removeFallbackFont(fallback: SDLTTFFont) {
        val fallbackPtr = (fallback as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("fallback is not a wasm SDL_ttf font")
        sdl_ttf_kmp_RemoveFallbackFont(check(), fallbackPtr)
    }

    override fun clearFallbackFonts() {
        sdl_ttf_kmp_ClearFallbackFonts(check())
    }

    override fun close() {
        val f = font
        if (f == 0) return
        font = 0
        sdl_ttf_kmp_CloseFont(f)
    }
}

internal class WasmTTFTextEngine(handle: Int, private val kind: Int) : SDLTTFTextEngine {

    internal var engine: Int = handle

    override val ptr: Long get() = if (engine == 0) 0L else engine.toLong()

    internal fun check(): Int =
        engine.also { if (it == 0) throw IllegalStateException("SDL_ttf text engine is closed") }

    override fun close() {
        val e = engine
        if (e == 0) return
        engine = 0
        when (kind) {
            SDLTTFTextEngineKind.RENDERER -> sdl_ttf_kmp_DestroyRendererTextEngine(e)
            SDLTTFTextEngineKind.SURFACE -> sdl_ttf_kmp_DestroySurfaceTextEngine(e)
            else -> sdl_ttf_kmp_DestroyGPUTextEngine(e)
        }
    }
}

internal object SDLTTFTextEngineKind {
    const val RENDERER = 0
    const val SURFACE = 1
    const val GPU = 2
}

internal class WasmTTFText(handle: Int) : SDLTTFText {

    internal var textHandle: Int = handle

    override val ptr: Long get() = if (textHandle == 0) 0L else textHandle.toLong()

    internal fun check(): Int =
        textHandle.also { if (it == 0) throw IllegalStateException("SDL_ttf text is closed") }

    override val text: String? get() = sdl_ttf_kmp_GetTextString(check())

    override val numLines: Int get() = sdl_ttf_kmp_GetTextNumLines(check())

    override var color: SDLColor
        get() {
            check(sdl_ttf_kmp_GetTextColor(check()) != 0) { "TTF_GetTextColor failed" }
            return SDLColor(r32(0), r32(1), r32(2), r32(3))
        }
        set(value) {
            sdl_ttf_kmp_SetTextColor(check(), value.r, value.g, value.b, value.a)
        }

    override var position: SDLPoint?
        get() = if (sdl_ttf_kmp_GetTextPosition(check()) == 0) null else SDLPoint(r32(0), r32(1))
        set(value) {
            sdl_ttf_kmp_SetTextPosition(check(), value?.x ?: 0, value?.y ?: 0)
        }

    override var wrapWidth: Int
        get() {
            sdl_ttf_kmp_GetTextWrapWidth(check())
            return r32(0)
        }
        set(value) {
            sdl_ttf_kmp_SetTextWrapWidth(check(), value)
        }

    override var wrapWhitespaceVisible: Boolean
        get() = sdl_ttf_kmp_TextWrapWhitespaceVisible(check()) != 0
        set(value) {
            sdl_ttf_kmp_SetTextWrapWhitespaceVisible(check(), if (value) 1 else 0)
        }

    override val size: SDLPoint?
        get() = if (sdl_ttf_kmp_GetTextSize(check()) == 0) null else SDLPoint(r32(0), r32(1))

    override fun draw(x: Float, y: Float): Boolean = sdl_ttf_kmp_DrawRendererText(check(), x, y) != 0

    override fun drawOnSurface(x: Int, y: Int, surface: SDLSurface): Boolean {
        if (surface.ptr == 0L) throw IllegalStateException("SDL surface is closed")
        return sdl_ttf_kmp_DrawSurfaceText(check(), x, y, surface.ptr.toInt()) != 0
    }

    override fun setText(text: String): Boolean = sdl_ttf_kmp_SetTextString(check(), text) != 0

    override fun append(text: String): Boolean = sdl_ttf_kmp_AppendTextString(check(), text) != 0

    override fun insert(offset: Int, text: String): Boolean =
        sdl_ttf_kmp_InsertTextString(check(), offset, text) != 0

    override fun delete(offset: Int, length: Int): Boolean =
        sdl_ttf_kmp_DeleteTextString(check(), offset, length) != 0

    override fun subStringAt(offset: Int): SDLTTFSubString? {
        if (sdl_ttf_kmp_GetTextSubString(check(), offset) == 0) return null
        return substringResult()
    }

    override fun subStringForLine(line: Int): SDLTTFSubString? {
        if (sdl_ttf_kmp_GetTextSubStringForLine(check(), line) == 0) return null
        return substringResult()
    }

    override fun subStringForPoint(x: Int, y: Int): SDLTTFSubString? {
        if (sdl_ttf_kmp_GetTextSubStringForPoint(check(), x, y) == 0) return null
        return substringResult()
    }

    private fun substringResult(): SDLTTFSubString = SDLTTFSubString(
        flags = r32(0),
        offset = r32(1),
        length = r32(2),
        lineIndex = r32(3),
        rect = SDLRect(r32(4), r32(5), r32(6), r32(7)),
    )

    // The bundled SDL3 build has no WebGPU backend, so GPU text engines can
    // never be created on wasm (see SDLGPU in sdl-kmp); this only exists to
    // satisfy the common API.
    override fun getGPUDrawData(): List<SDLTTFGPUAtlasDrawSequence>? = null

    override fun close() {
        val t = textHandle
        if (t == 0) return
        textHandle = 0
        sdl_ttf_kmp_DestroyText(t)
    }
}

actual object SDLTTF {

    actual fun init(): Boolean = sdl_ttf_kmp_Init() != 0

    actual fun quit() {
        sdl_ttf_kmp_Quit()
    }

    actual fun wasInit(): Int = sdl_ttf_kmp_WasInit()

    actual fun version(): Int = sdl_ttf_kmp_Version()

    actual fun getFreeTypeVersion(): SDLTTFVersion {
        sdl_ttf_kmp_GetFreeTypeVersion()
        return SDLTTFVersion(r32(0), r32(1), r32(2))
    }

    actual fun getHarfBuzzVersion(): SDLTTFVersion {
        sdl_ttf_kmp_GetHarfBuzzVersion()
        return SDLTTFVersion(r32(0), r32(1), r32(2))
    }

    actual fun error(): String? = SDL.error()

    actual fun clearError() {
        SDL.clearError()
    }

    actual fun openFont(path: String, pointSize: Float): SDLTTFFont {
        val font = sdl_ttf_kmp_OpenFont(path, pointSize)
        if (font == 0) throw IllegalStateException("TTF_OpenFont failed: ${error()}")
        return WasmTTFFont(font)
    }

    actual fun openFontIO(stream: SDLIOStream, closeIO: Boolean, pointSize: Float): SDLTTFFont {
        if (stream.ptr == 0L) throw IllegalStateException("SDL IO stream is closed")
        val font = sdl_ttf_kmp_OpenFontIO(stream.ptr.toInt(), if (closeIO) 1 else 0, pointSize)
        if (font == 0) throw IllegalStateException("TTF_OpenFontIO failed: ${error()}")
        return WasmTTFFont(font)
    }

    actual fun copyFont(existingFont: SDLTTFFont): SDLTTFFont {
        val existing = (existingFont as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("existingFont is not a wasm SDL_ttf font")
        val font = sdl_ttf_kmp_CopyFont(existing)
        if (font == 0) throw IllegalStateException("TTF_CopyFont failed: ${error()}")
        return WasmTTFFont(font)
    }

    actual fun createRendererTextEngine(renderer: SDLRenderer): SDLTTFTextEngine {
        if (renderer.ptr == 0L) throw IllegalStateException("SDL renderer is closed")
        val engine = sdl_ttf_kmp_CreateRendererTextEngine(renderer.ptr.toInt())
        if (engine == 0) throw IllegalStateException("TTF_CreateRendererTextEngine failed: ${error()}")
        return WasmTTFTextEngine(engine, SDLTTFTextEngineKind.RENDERER)
    }

    actual fun createSurfaceTextEngine(): SDLTTFTextEngine {
        val engine = sdl_ttf_kmp_CreateSurfaceTextEngine()
        if (engine == 0) throw IllegalStateException("TTF_CreateSurfaceTextEngine failed: ${error()}")
        return WasmTTFTextEngine(engine, SDLTTFTextEngineKind.SURFACE)
    }

    actual fun createGPUTextEngine(device: SDLGPUDevice): SDLTTFTextEngine {
        if (device.ptr == 0L) throw IllegalStateException("SDL GPU device is closed")
        val engine = sdl_ttf_kmp_CreateGPUTextEngine(device.ptr.toInt())
        if (engine == 0) throw IllegalStateException("TTF_CreateGPUTextEngine failed: ${error()}")
        return WasmTTFTextEngine(engine, SDLTTFTextEngineKind.GPU)
    }

    actual fun createText(engine: SDLTTFTextEngine?, font: SDLTTFFont?, text: String): SDLTTFText {
        val enginePtr = (engine as? WasmTTFTextEngine)?.engine ?: 0
        val fontPtr = (font as? WasmTTFFont)?.font ?: 0
        val textHandle = sdl_ttf_kmp_CreateText(enginePtr, fontPtr, text)
        if (textHandle == 0) throw IllegalStateException("TTF_CreateText failed: ${error()}")
        return WasmTTFText(textHandle)
    }

    private fun surfaceOf(handle: Int): SDLSurface? =
        if (handle == 0) null else WasmTTFSurface(handle, owned = true)

    actual fun renderTextSolid(font: SDLTTFFont, text: String, fg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a wasm SDL_ttf font")
        return surfaceOf(sdl_ttf_kmp_RenderTextSolid(f, text, wrapWidth, fg.r, fg.g, fg.b, fg.a))
    }

    actual fun renderTextShaded(font: SDLTTFFont, text: String, fg: SDLColor, bg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a wasm SDL_ttf font")
        return surfaceOf(
            sdl_ttf_kmp_RenderTextShaded(
                f, text, wrapWidth, fg.r, fg.g, fg.b, fg.a, bg.r, bg.g, bg.b, bg.a,
            ),
        )
    }

    actual fun renderTextBlended(font: SDLTTFFont, text: String, fg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a wasm SDL_ttf font")
        return surfaceOf(sdl_ttf_kmp_RenderTextBlended(f, text, wrapWidth, fg.r, fg.g, fg.b, fg.a))
    }

    actual fun renderTextLCD(font: SDLTTFFont, text: String, fg: SDLColor, bg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a wasm SDL_ttf font")
        return surfaceOf(
            sdl_ttf_kmp_RenderTextLCD(
                f, text, wrapWidth, fg.r, fg.g, fg.b, fg.a, bg.r, bg.g, bg.b, bg.a,
            ),
        )
    }

    actual fun renderGlyphSolid(font: SDLTTFFont, ch: Int, fg: SDLColor): SDLSurface? {
        val f = (font as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a wasm SDL_ttf font")
        return surfaceOf(sdl_ttf_kmp_RenderGlyphSolid(f, ch, fg.r, fg.g, fg.b, fg.a))
    }

    actual fun renderGlyphShaded(font: SDLTTFFont, ch: Int, fg: SDLColor, bg: SDLColor): SDLSurface? {
        val f = (font as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a wasm SDL_ttf font")
        return surfaceOf(
            sdl_ttf_kmp_RenderGlyphShaded(f, ch, fg.r, fg.g, fg.b, fg.a, bg.r, bg.g, bg.b, bg.a),
        )
    }

    actual fun renderGlyphBlended(font: SDLTTFFont, ch: Int, fg: SDLColor): SDLSurface? {
        val f = (font as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a wasm SDL_ttf font")
        return surfaceOf(sdl_ttf_kmp_RenderGlyphBlended(f, ch, fg.r, fg.g, fg.b, fg.a))
    }

    actual fun renderGlyphLCD(font: SDLTTFFont, ch: Int, fg: SDLColor, bg: SDLColor): SDLSurface? {
        val f = (font as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a wasm SDL_ttf font")
        return surfaceOf(
            sdl_ttf_kmp_RenderGlyphLCD(f, ch, fg.r, fg.g, fg.b, fg.a, bg.r, bg.g, bg.b, bg.a),
        )
    }

    actual fun getGlyphImage(font: SDLTTFFont, ch: Int): SDLSurface? {
        val f = (font as? WasmTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a wasm SDL_ttf font")
        return surfaceOf(sdl_ttf_kmp_GetGlyphImage(f, ch))
    }
}
