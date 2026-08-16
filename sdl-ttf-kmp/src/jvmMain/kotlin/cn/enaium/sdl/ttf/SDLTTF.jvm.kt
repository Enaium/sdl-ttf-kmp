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
import cn.enaium.sdl.SDLIOStream
import cn.enaium.sdl.SDLPoint
import cn.enaium.sdl.SDLRect
import cn.enaium.sdl.SDLRenderer
import cn.enaium.sdl.SDLScaleMode
import cn.enaium.sdl.SDLSurface

/**
 * An [SDLSurface] wrapping an SDL_Surface created outside the sdl-kmp
 * library (e.g. by SDL_ttf's render functions). All operations delegate to
 * the TTF JNI library, which shares the SDL3 instance with libsdl_jni.
 */
internal class JvmTTFSurface internal constructor(
    ptr: Long,
    internal val owned: Boolean,
) : SDLSurface {

    internal var surface: Long = ptr

    override val ptr: Long get() = surface

    internal fun check(): Long =
        surface.also { if (it == 0L) throw IllegalStateException("SDL surface is closed") }

    override val width: Int get() = Jni.surfaceWidth(check())
    override val height: Int get() = Jni.surfaceHeight(check())
    override val format: Int get() = Jni.surfaceFormat(check())
    override val colorspace: Int get() = Jni.surfaceColorspace(check())
    override val pitch: Int get() = Jni.surfacePitch(check())

    override val pixels: ByteArray
        get() = Jni.surfacePixels(check()) ?: ByteArray(0)

    override fun lock(): Boolean = Jni.lockSurface(check())

    override fun unlock() {
        Jni.unlockSurface(check())
    }

    override fun fillRect(rect: SDLRect?, color: SDLColor): Boolean {
        return Jni.surfaceFillRect(
            check(),
            rect?.let { intArrayOf(it.x, it.y, it.width, it.height) },
            color.r, color.g, color.b, color.a,
        )
    }

    override fun fillRects(rects: List<SDLRect>, color: SDLColor): Boolean {
        if (rects.isEmpty()) return true
        val arr = IntArray(rects.size * 4)
        for (i in rects.indices) {
            arr[i * 4] = rects[i].x
            arr[i * 4 + 1] = rects[i].y
            arr[i * 4 + 2] = rects[i].width
            arr[i * 4 + 3] = rects[i].height
        }
        return Jni.surfaceFillRects(check(), arr, color.r, color.g, color.b, color.a)
    }

    override fun blit(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?): Boolean {
        if (dst.ptr == 0L) throw IllegalArgumentException("dst is closed")
        return Jni.surfaceBlit(
            check(),
            src?.let { intArrayOf(it.x, it.y, it.width, it.height) },
            dst.ptr,
            dstRect?.let { intArrayOf(it.x, it.y, it.width, it.height) },
        )
    }

    override fun blitScaled(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?, scaleMode: Int): Boolean {
        if (dst.ptr == 0L) throw IllegalArgumentException("dst is closed")
        return Jni.surfaceBlitScaled(
            check(),
            src?.let { intArrayOf(it.x, it.y, it.width, it.height) },
            dst.ptr,
            dstRect?.let { intArrayOf(it.x, it.y, it.width, it.height) },
            scaleMode,
        )
    }

    override fun saveBMP(path: String): Boolean = Jni.surfaceSaveBMP(check(), path)

    override fun convert(format: Int): SDLSurface {
        val converted = Jni.convertSurface(check(), format)
        check(converted != 0L) { "SDL_ConvertSurface failed: ${SDL.error()}" }
        return JvmTTFSurface(converted, owned = true)
    }

    override fun close() {
        val s = surface
        if (s == 0L) return
        surface = 0L
        if (owned) {
            Jni.destroySurface(s)
        }
    }
}

internal actual fun Long.toSDLSurface(owned: Boolean): SDLSurface? =
    if (this == 0L) null else JvmTTFSurface(this, owned = owned)

internal class JvmSDLTTFFont internal constructor(ptr: Long) : SDLTTFFont {

    internal var font: Long = ptr

    override val ptr: Long get() = font

    internal fun check(): Long =
        font.also { if (it == 0L) throw IllegalStateException("SDL_ttf font is closed") }

    override var size: Float
        get() = Jni.getFontSize(check())
        set(value) {
            Jni.setFontSize(check(), value)
        }

    override var style: Int
        get() = Jni.getFontStyle(check())
        set(value) {
            Jni.setFontStyle(check(), value)
        }

    override var outline: Int
        get() = Jni.getFontOutline(check())
        set(value) {
            Jni.setFontOutline(check(), value)
        }

    override var hinting: Int
        get() = Jni.getFontHinting(check())
        set(value) {
            Jni.setFontHinting(check(), value)
        }

    override var SDF: Boolean
        get() = Jni.getFontSDF(check())
        set(value) {
            Jni.setFontSDF(check(), value)
        }

    override var wrapAlignment: Int
        get() = Jni.getFontWrapAlignment(check())
        set(value) {
            Jni.setFontWrapAlignment(check(), value)
        }

    override var lineSkip: Int
        get() = Jni.getFontLineSkip(check())
        set(value) {
            Jni.setFontLineSkip(check(), value)
        }

    override var kerning: Boolean
        get() = Jni.getFontKerning(check())
        set(value) {
            Jni.setFontKerning(check(), value)
        }

    override var direction: Int
        get() = Jni.getFontDirection(check())
        set(value) {
            Jni.setFontDirection(check(), value)
        }

    override var charSpacing: Int
        get() = Jni.getFontCharSpacing(check())
        set(value) {
            Jni.setFontCharSpacing(check(), value)
        }

    override val height: Int get() = Jni.getFontHeight(check())
    override val ascent: Int get() = Jni.getFontAscent(check())
    override val descent: Int get() = Jni.getFontDescent(check())
    override val familyName: String? get() = Jni.getFontFamilyName(check())
    override val styleName: String? get() = Jni.getFontStyleName(check())
    override val fixedWidth: Boolean get() = Jni.fontIsFixedWidth(check())
    override val scalable: Boolean get() = Jni.fontIsScalable(check())
    override val weight: Int get() = Jni.getFontWeight(check())

    override fun hasGlyph(ch: Int): Boolean = Jni.fontHasGlyph(check(), ch)

    override fun getGlyphMetrics(ch: Int): SDLTTFGlyphMetrics? =
        Jni.getGlyphMetrics(check(), ch)?.let {
            SDLTTFGlyphMetrics(it[0], it[1], it[2], it[3], it[4])
        }

    override fun getGlyphKerning(previousCh: Int, ch: Int): Int? =
        Jni.getGlyphKerning(check(), previousCh, ch)?.first()

    override fun getStringSize(text: String): SDLPoint? =
        Jni.getStringSize(check(), text, 0)?.let { SDLPoint(it[0], it[1]) }

    override fun getStringSizeWrapped(text: String, wrapWidth: Int): SDLPoint? =
        Jni.getStringSizeWrapped(check(), text, 0, wrapWidth)?.let { SDLPoint(it[0], it[1]) }

    override fun measureString(text: String, maxWidth: Int): SDLTTFMeasure? =
        Jni.measureString(check(), text, 0, maxWidth)?.let { SDLTTFMeasure(it[0], it[1]) }

    override fun addFallbackFont(fallback: SDLTTFFont): Boolean {
        val fallbackPtr = (fallback as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("fallback is not a JVM SDL_ttf font")
        return Jni.addFallbackFont(check(), fallbackPtr)
    }

    override fun removeFallbackFont(fallback: SDLTTFFont) {
        val fallbackPtr = (fallback as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("fallback is not a JVM SDL_ttf font")
        Jni.removeFallbackFont(check(), fallbackPtr)
    }

    override fun clearFallbackFonts() {
        Jni.clearFallbackFonts(check())
    }

    override fun close() {
        val f = font
        if (f == 0L) return
        font = 0L
        Jni.closeFont(f)
    }
}

internal class JvmSDLTTFTextEngine internal constructor(
    ptr: Long,
    private val rendererEngine: Boolean,
) : SDLTTFTextEngine {

    internal var engine: Long = ptr

    override val ptr: Long get() = engine

    internal fun check(): Long =
        engine.also { if (it == 0L) throw IllegalStateException("SDL_ttf text engine is closed") }

    override fun close() {
        val e = engine
        if (e == 0L) return
        engine = 0L
        if (rendererEngine) {
            Jni.destroyRendererTextEngine(e)
        } else {
            Jni.destroySurfaceTextEngine(e)
        }
    }
}

internal class JvmSDLTTFText internal constructor(
    ptr: Long,
    private val engine: JvmSDLTTFTextEngine?,
    private val font: JvmSDLTTFFont?,
) : SDLTTFText {

    internal var textPtr: Long = ptr

    override val ptr: Long get() = textPtr

    internal fun check(): Long =
        textPtr.also { if (it == 0L) throw IllegalStateException("SDL_ttf text is closed") }

    override val text: String?
        get() = Jni.getTextString(check())

    override val numLines: Int
        get() = Jni.getTextNumLines(check())

    override var color: SDLColor
        get() {
            val c = Jni.getTextColor(check())
                ?: throw IllegalStateException("SDL_ttf TTF_GetTextColor failed: ${SDLTTF.error()}")
            return SDLColor(c[0], c[1], c[2], c[3])
        }
        set(value) {
            Jni.setTextColor(check(), value.r, value.g, value.b, value.a)
        }

    override var position: SDLPoint?
        get() = Jni.getTextPosition(check())?.let { SDLPoint(it[0], it[1]) }
        set(value) {
            Jni.setTextPosition(check(), value?.x ?: 0, value?.y ?: 0)
        }

    override var wrapWidth: Int
        get() = Jni.getTextWrapWidth(check())
        set(value) {
            Jni.setTextWrapWidth(check(), value)
        }

    override var wrapWhitespaceVisible: Boolean
        get() = Jni.textWrapWhitespaceVisible(check())
        set(value) {
            Jni.setTextWrapWhitespaceVisible(check(), value)
        }

    override val size: SDLPoint?
        get() = Jni.getTextSize(check())?.let { SDLPoint(it[0], it[1]) }

    override fun draw(x: Float, y: Float): Boolean = Jni.drawRendererText(check(), x, y)

    override fun drawOnSurface(x: Int, y: Int, surface: SDLSurface): Boolean {
        if (surface.ptr == 0L) throw IllegalStateException("SDL surface is closed")
        return Jni.drawSurfaceText(check(), x, y, surface.ptr)
    }

    override fun setText(text: String): Boolean = Jni.setTextString(check(), text, 0)

    override fun append(text: String): Boolean = Jni.appendTextString(check(), text, 0)

    override fun insert(offset: Int, text: String): Boolean = Jni.insertTextString(check(), offset, text, 0)

    override fun delete(offset: Int, length: Int): Boolean = Jni.deleteTextString(check(), offset, length)

    override fun subStringAt(offset: Int): SDLTTFSubString? =
        Jni.getTextSubString(check(), offset)?.let { it.toSubString() }

    override fun subStringForLine(line: Int): SDLTTFSubString? =
        Jni.getTextSubStringForLine(check(), line)?.let { it.toSubString() }

    override fun subStringForPoint(x: Int, y: Int): SDLTTFSubString? =
        Jni.getTextSubStringForPoint(check(), x, y)?.let { it.toSubString() }

    override fun close() {
        val t = textPtr
        if (t == 0L) return
        textPtr = 0L
        Jni.destroyText(t)
    }
}

private fun IntArray.toSubString(): SDLTTFSubString = SDLTTFSubString(
    flags = this[0],
    offset = this[1],
    length = this[2],
    lineIndex = this[3],
    rect = SDLRect(this[5], this[6], this[7], this[8]),
)

actual object SDLTTF {

    actual fun init(): Boolean = Jni.init()

    actual fun quit() {
        Jni.quit()
    }

    actual fun wasInit(): Int = Jni.wasInit()

    actual fun version(): Int = Jni.version()

    actual fun getFreeTypeVersion(): SDLTTFVersion {
        val v = Jni.getFreeTypeVersion()
        return SDLTTFVersion(v[0], v[1], v[2])
    }

    actual fun getHarfBuzzVersion(): SDLTTFVersion {
        val v = Jni.getHarfBuzzVersion()
        return SDLTTFVersion(v[0], v[1], v[2])
    }

    actual fun error(): String? = Jni.getError()?.takeIf { it.isNotEmpty() }

    actual fun clearError() {
        Jni.clearError()
    }

    actual fun openFont(path: String, pointSize: Float): SDLTTFFont {
        val font = Jni.openFont(path, pointSize)
        check(font != 0L) { "TTF_OpenFont failed: ${SDLTTF.error()}" }
        return JvmSDLTTFFont(font)
    }

    actual fun openFontIO(stream: SDLIOStream, closeIO: Boolean, pointSize: Float): SDLTTFFont {
        if (stream.ptr == 0L) throw IllegalStateException("SDL IO stream is closed")
        val font = Jni.openFontIO(stream.ptr, closeIO, pointSize)
        check(font != 0L) { "TTF_OpenFontIO failed: ${SDLTTF.error()}" }
        return JvmSDLTTFFont(font)
    }

    actual fun copyFont(existingFont: SDLTTFFont): SDLTTFFont {
        val existing = (existingFont as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("existingFont is not a JVM SDL_ttf font")
        val font = Jni.copyFont(existing)
        check(font != 0L) { "TTF_CopyFont failed: ${SDLTTF.error()}" }
        return JvmSDLTTFFont(font)
    }

    actual fun createRendererTextEngine(renderer: SDLRenderer): SDLTTFTextEngine {
        if (renderer.ptr == 0L) throw IllegalStateException("SDL renderer is closed")
        val engine = Jni.createRendererTextEngine(renderer.ptr)
        check(engine != 0L) { "TTF_CreateRendererTextEngine failed: ${SDLTTF.error()}" }
        return JvmSDLTTFTextEngine(engine, rendererEngine = true)
    }

    actual fun createSurfaceTextEngine(): SDLTTFTextEngine {
        val engine = Jni.createSurfaceTextEngine()
        check(engine != 0L) { "TTF_CreateSurfaceTextEngine failed: ${SDLTTF.error()}" }
        return JvmSDLTTFTextEngine(engine, rendererEngine = false)
    }

    actual fun createText(engine: SDLTTFTextEngine?, font: SDLTTFFont?, text: String): SDLTTFText {
        val enginePtr = (engine as? JvmSDLTTFTextEngine)?.engine ?: 0L
        val fontPtr = (font as? JvmSDLTTFFont)?.font ?: 0L
        val t = Jni.createText(enginePtr, fontPtr, text, 0)
        check(t != 0L) { "TTF_CreateText failed: ${SDLTTF.error()}" }
        return JvmSDLTTFText(t, engine as? JvmSDLTTFTextEngine, font as? JvmSDLTTFFont)
    }

    private fun surfaceOf(ptr: Long): SDLSurface? = ptr.toSDLSurface(owned = true)

    actual fun renderTextSolid(font: SDLTTFFont, text: String, fg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a JVM SDL_ttf font")
        val ptr = if (wrapWidth > 0) {
            Jni.renderTextSolidWrapped(f, text, 0, fg.r, fg.g, fg.b, fg.a, wrapWidth)
        } else {
            Jni.renderTextSolid(f, text, 0, fg.r, fg.g, fg.b, fg.a)
        }
        return surfaceOf(ptr)
    }

    actual fun renderTextShaded(font: SDLTTFFont, text: String, fg: SDLColor, bg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a JVM SDL_ttf font")
        val ptr = if (wrapWidth > 0) {
            Jni.renderTextShadedWrapped(f, text, 0, fg.r, fg.g, fg.b, fg.a, bg.r, bg.g, bg.b, bg.a, wrapWidth)
        } else {
            Jni.renderTextShaded(f, text, 0, fg.r, fg.g, fg.b, fg.a, bg.r, bg.g, bg.b, bg.a)
        }
        return surfaceOf(ptr)
    }

    actual fun renderTextBlended(font: SDLTTFFont, text: String, fg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a JVM SDL_ttf font")
        val ptr = if (wrapWidth > 0) {
            Jni.renderTextBlendedWrapped(f, text, 0, fg.r, fg.g, fg.b, fg.a, wrapWidth)
        } else {
            Jni.renderTextBlended(f, text, 0, fg.r, fg.g, fg.b, fg.a)
        }
        return surfaceOf(ptr)
    }

    actual fun renderTextLCD(font: SDLTTFFont, text: String, fg: SDLColor, bg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a JVM SDL_ttf font")
        val ptr = if (wrapWidth > 0) {
            Jni.renderTextLCDWrapped(f, text, 0, fg.r, fg.g, fg.b, fg.a, bg.r, bg.g, bg.b, bg.a, wrapWidth)
        } else {
            Jni.renderTextLCD(f, text, 0, fg.r, fg.g, fg.b, fg.a, bg.r, bg.g, bg.b, bg.a)
        }
        return surfaceOf(ptr)
    }

    actual fun renderGlyphSolid(font: SDLTTFFont, ch: Int, fg: SDLColor): SDLSurface? {
        val f = (font as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a JVM SDL_ttf font")
        return surfaceOf(Jni.renderGlyphSolid(f, ch, fg.r, fg.g, fg.b, fg.a))
    }

    actual fun renderGlyphShaded(font: SDLTTFFont, ch: Int, fg: SDLColor, bg: SDLColor): SDLSurface? {
        val f = (font as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a JVM SDL_ttf font")
        return surfaceOf(Jni.renderGlyphShaded(f, ch, fg.r, fg.g, fg.b, fg.a, bg.r, bg.g, bg.b, bg.a))
    }

    actual fun renderGlyphBlended(font: SDLTTFFont, ch: Int, fg: SDLColor): SDLSurface? {
        val f = (font as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a JVM SDL_ttf font")
        return surfaceOf(Jni.renderGlyphBlended(f, ch, fg.r, fg.g, fg.b, fg.a))
    }

    actual fun renderGlyphLCD(font: SDLTTFFont, ch: Int, fg: SDLColor, bg: SDLColor): SDLSurface? {
        val f = (font as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a JVM SDL_ttf font")
        return surfaceOf(Jni.renderGlyphLCD(f, ch, fg.r, fg.g, fg.b, fg.a, bg.r, bg.g, bg.b, bg.a))
    }

    actual fun getGlyphImage(font: SDLTTFFont, ch: Int): SDLSurface? {
        val f = (font as? JvmSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a JVM SDL_ttf font")
        return surfaceOf(Jni.getGlyphImage(f, ch))
    }
}
