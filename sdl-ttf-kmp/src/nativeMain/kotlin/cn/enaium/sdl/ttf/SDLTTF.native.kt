@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cn.enaium.sdl.ttf

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLColor
import cn.enaium.sdl.SDLIOStream
import cn.enaium.sdl.SDLPoint
import cn.enaium.sdl.SDLRect
import cn.enaium.sdl.SDLRenderer
import cn.enaium.sdl.SDLSurface
import cnames.structs.SDL_IOStream
import cnames.structs.SDL_Renderer
import cnames.structs.TTF_Font
import cnames.structs.TTF_TextEngine
import kotlinx.cinterop.*
import platform.posix.size_tVar
import sdl3.*
import sdl_ttf.TTF_AddFallbackFont
import sdl_ttf.TTF_AppendTextString
import sdl_ttf.TTF_ClearFallbackFonts
import sdl_ttf.TTF_CloseFont
import sdl_ttf.TTF_CopyFont
import sdl_ttf.TTF_CreateRendererTextEngine
import sdl_ttf.TTF_CreateSurfaceTextEngine
import sdl_ttf.TTF_CreateText
import sdl_ttf.TTF_DeleteTextString
import sdl_ttf.TTF_DestroyRendererTextEngine
import sdl_ttf.TTF_DestroySurfaceTextEngine
import sdl_ttf.TTF_DestroyText
import sdl_ttf.TTF_DrawRendererText
import sdl_ttf.TTF_DrawSurfaceText
import sdl_ttf.TTF_FontHasGlyph
import sdl_ttf.TTF_FontIsFixedWidth
import sdl_ttf.TTF_FontIsScalable
import sdl_ttf.TTF_GetFontAscent
import sdl_ttf.TTF_GetFontCharSpacing
import sdl_ttf.TTF_GetFontDPI
import sdl_ttf.TTF_GetFontDescent
import sdl_ttf.TTF_GetFontDirection
import sdl_ttf.TTF_GetFontFamilyName
import sdl_ttf.TTF_GetFontHeight
import sdl_ttf.TTF_GetFontHinting
import sdl_ttf.TTF_GetFontKerning
import sdl_ttf.TTF_GetFontLineSkip
import sdl_ttf.TTF_GetFontOutline
import sdl_ttf.TTF_GetFontSDF
import sdl_ttf.TTF_GetFontSize
import sdl_ttf.TTF_GetFontStyle
import sdl_ttf.TTF_GetFontStyleName
import sdl_ttf.TTF_GetFontWeight
import sdl_ttf.TTF_GetFontWrapAlignment
import sdl_ttf.TTF_GetFreeTypeVersion
import sdl_ttf.TTF_GetGlyphImage
import sdl_ttf.TTF_GetGlyphKerning
import sdl_ttf.TTF_GetGlyphMetrics
import sdl_ttf.TTF_GetHarfBuzzVersion
import sdl_ttf.TTF_GetStringSize
import sdl_ttf.TTF_GetStringSizeWrapped
import sdl_ttf.TTF_GetTextColor
import sdl_ttf.TTF_GetTextDirection
import sdl_ttf.TTF_GetTextPosition
import sdl_ttf.TTF_GetTextSize
import sdl_ttf.TTF_GetTextSubString
import sdl_ttf.TTF_GetTextSubStringForLine
import sdl_ttf.TTF_GetTextSubStringForPoint
import sdl_ttf.TTF_GetTextWrapWidth
import sdl_ttf.TTF_Init
import sdl_ttf.TTF_InsertTextString
import sdl_ttf.TTF_MeasureString
import sdl_ttf.TTF_OpenFont
import sdl_ttf.TTF_OpenFontIO
import sdl_ttf.TTF_Quit
import sdl_ttf.TTF_RemoveFallbackFont
import sdl_ttf.TTF_RenderGlyph_Blended
import sdl_ttf.TTF_RenderGlyph_LCD
import sdl_ttf.TTF_RenderGlyph_Shaded
import sdl_ttf.TTF_RenderGlyph_Solid
import sdl_ttf.TTF_RenderText_Blended
import sdl_ttf.TTF_RenderText_Blended_Wrapped
import sdl_ttf.TTF_RenderText_LCD
import sdl_ttf.TTF_RenderText_LCD_Wrapped
import sdl_ttf.TTF_RenderText_Shaded
import sdl_ttf.TTF_RenderText_Shaded_Wrapped
import sdl_ttf.TTF_RenderText_Solid
import sdl_ttf.TTF_RenderText_Solid_Wrapped
import sdl_ttf.TTF_SetFontCharSpacing
import sdl_ttf.TTF_SetFontDirection
import sdl_ttf.TTF_SetFontHinting
import sdl_ttf.TTF_SetFontKerning
import sdl_ttf.TTF_SetFontLineSkip
import sdl_ttf.TTF_SetFontOutline
import sdl_ttf.TTF_SetFontSDF
import sdl_ttf.TTF_SetFontSize
import sdl_ttf.TTF_SetFontSizeDPI
import sdl_ttf.TTF_SetFontStyle
import sdl_ttf.TTF_SetFontWrapAlignment
import sdl_ttf.TTF_SetTextColor
import sdl_ttf.TTF_SetTextDirection
import sdl_ttf.TTF_SetTextEngine
import sdl_ttf.TTF_SetTextFont
import sdl_ttf.TTF_SetTextPosition
import sdl_ttf.TTF_SetTextString
import sdl_ttf.TTF_SetTextWrapWhitespaceVisible
import sdl_ttf.TTF_SetTextWrapWidth
import sdl_ttf.TTF_SubString
import sdl_ttf.TTF_Text
import sdl_ttf.TTF_TextWrapWhitespaceVisible
import sdl_ttf.TTF_UpdateText
import sdl_ttf.TTF_Version
import sdl_ttf.TTF_WasInit



private fun SDLColor.toCValue(): CValue<SDL_Color> = cValue {
    r = this@toCValue.r.toUByte()
    g = this@toCValue.g.toUByte()
    b = this@toCValue.b.toUByte()
    a = this@toCValue.a.toUByte()
}

private fun CPointer<TTF_Font>.fontOrNull(): CPointer<TTF_Font>? = this

/**
 * An [SDLSurface] wrapping an SDL_Surface created outside the sdl-kmp
 * library (e.g. by SDL_ttf's render functions). The SDL3 calls come from the
 * sdl3 cinterop package of the sdl-kmp klib, so they operate on the very
 * SDL3 instance the application uses.
 */
internal class NativeTTFSurface internal constructor(
    ptr: CPointer<SDL_Surface>?,
    internal val owned: Boolean,
) : SDLSurface {

    internal var surface: CPointer<SDL_Surface>? = ptr

    override val ptr: Long get() = surface?.rawValue?.toLong() ?: 0L

    internal fun check(): CPointer<SDL_Surface> =
        surface ?: throw IllegalStateException("SDL surface is closed")

    override val width: Int get() = check().pointed.w
    override val height: Int get() = check().pointed.h
    override val format: Int get() = check().pointed.format.toInt()
    override val colorspace: Int get() = sdl3.SDL_GetSurfaceColorspace(check()).toInt()
    override val pitch: Int get() = check().pointed.pitch

    override val pixels: ByteArray
        get() {
            val bytes = pitch * height
            val out = ByteArray(bytes)
            copyBytes(out, 0, check().pointed.pixels, bytes)
            return out
        }

    override fun lock(): Boolean = sdl3.SDL_LockSurface(check())

    override fun unlock() {
        sdl3.SDL_UnlockSurface(check())
    }

    override fun fillRect(rect: SDLRect?, color: SDLColor): Boolean = memScoped {
        val rectPtr = rect?.let {
            val r = alloc<sdl3.SDL_Rect>()
            r.x = it.x
            r.y = it.y
            r.w = it.width
            r.h = it.height
            r.ptr
        }
        sdl3.SDL_FillSurfaceRect(check(), rectPtr, color.toMappedPixel())
    }

    override fun fillRects(rects: List<SDLRect>, color: SDLColor): Boolean = memScoped {
        if (rects.isEmpty()) return true
        val arr = allocArray<sdl3.SDL_Rect>(rects.size)
        for (i in rects.indices) {
            arr[i].x = rects[i].x
            arr[i].y = rects[i].y
            arr[i].w = rects[i].width
            arr[i].h = rects[i].height
        }
        sdl3.SDL_FillSurfaceRects(check(), arr, rects.size, color.toMappedPixel())
    }

    override fun blit(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?): Boolean {
        if (dst.ptr == 0L) throw IllegalArgumentException("dst is closed")
        return memScoped {
            val srcPtr = src?.let {
                val r = alloc<sdl3.SDL_Rect>()
                r.x = it.x
                r.y = it.y
                r.w = it.width
                r.h = it.height
                r.ptr
            }
            val dstPtr = dstRect?.let {
                val r = alloc<sdl3.SDL_Rect>()
                r.x = it.x
                r.y = it.y
                r.w = it.width
                r.h = it.height
                r.ptr
            }
            sdl3.SDL_BlitSurface(check(), srcPtr, dst.ptr.toCPointer<SDL_Surface>(), dstPtr)
        }
    }

    override fun blitScaled(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?, scaleMode: Int): Boolean {
        if (dst.ptr == 0L) throw IllegalArgumentException("dst is closed")
        return memScoped {
            val srcPtr = src?.let {
                val r = alloc<sdl3.SDL_Rect>()
                r.x = it.x
                r.y = it.y
                r.w = it.width
                r.h = it.height
                r.ptr
            }
            val dstPtr = dstRect?.let {
                val r = alloc<sdl3.SDL_Rect>()
                r.x = it.x
                r.y = it.y
                r.w = it.width
                r.h = it.height
                r.ptr
            }
            sdl3.SDL_BlitSurfaceScaled(
                check(), srcPtr,
                dst.ptr.toCPointer<SDL_Surface>(), dstPtr,
                scaleMode,
            )
        }
    }

    override fun saveBMP(path: String): Boolean = sdl3.SDL_SaveBMP(check(), path)

    override fun convert(format: Int): SDLSurface {
        val converted = sdl3.SDL_ConvertSurface(check(), format.toUInt())
            ?: throw IllegalStateException("SDL_ConvertSurface failed: ${SDL.error()}")
        return NativeTTFSurface(converted, owned = true)
    }

    override fun close() {
        val s = surface
        if (s == null) return
        surface = null
        if (owned) {
            sdl3.SDL_DestroySurface(s)
        }
    }
}

private fun SDLColor.toMappedPixel(): UInt =
    (r.toUInt() shl 24) or (g.toUInt() shl 16) or (b.toUInt() shl 8) or a.toUInt()

private fun copyBytes(dst: ByteArray, dstOffset: Int, src: CPointer<out CPointed>?, length: Int) {
    if (src == null) return
    val bytes = src.reinterpret<ByteVar>()
    for (i in 0 until length) {
        dst[dstOffset + i] = bytes[i]
    }
}

internal actual fun Long.toSDLSurface(owned: Boolean): SDLSurface? {
    if (this == 0L) return null
    return NativeTTFSurface(this.toCPointer<SDL_Surface>(), owned = owned)
}

internal class NativeSDLTTFFont internal constructor(
    ptr: CPointer<TTF_Font>?,
) : SDLTTFFont {

    internal var font: CPointer<TTF_Font>? = ptr

    override val ptr: Long get() = font?.rawValue?.toLong() ?: 0L

    internal fun check(): CPointer<TTF_Font> =
        font ?: throw IllegalStateException("SDL_ttf font is closed")

    override var size: Float
        get() = TTF_GetFontSize(check())
        set(value) {
            TTF_SetFontSize(check(), value)
        }

    override var style: Int
        get() = TTF_GetFontStyle(check()).toInt()
        set(value) {
            TTF_SetFontStyle(check(), value.toUInt())
        }

    override var outline: Int
        get() = TTF_GetFontOutline(check())
        set(value) {
            TTF_SetFontOutline(check(), value)
        }

    override var hinting: Int
        get() = TTF_GetFontHinting(check())
        set(value) {
            TTF_SetFontHinting(check(), value)
        }

    override var SDF: Boolean
        get() = TTF_GetFontSDF(check())
        set(value) {
            TTF_SetFontSDF(check(), value)
        }

    override var wrapAlignment: Int
        get() = TTF_GetFontWrapAlignment(check())
        set(value) {
            TTF_SetFontWrapAlignment(check(), value)
        }

    override var lineSkip: Int
        get() = TTF_GetFontLineSkip(check())
        set(value) {
            TTF_SetFontLineSkip(check(), value)
        }

    override var kerning: Boolean
        get() = TTF_GetFontKerning(check())
        set(value) {
            TTF_SetFontKerning(check(), value)
        }

    override var direction: Int
        get() = TTF_GetFontDirection(check()).toInt()
        set(value) {
            TTF_SetFontDirection(check(), value.toUInt())
        }

    override var charSpacing: Int
        get() = TTF_GetFontCharSpacing(check())
        set(value) {
            TTF_SetFontCharSpacing(check(), value)
        }

    override val height: Int get() = TTF_GetFontHeight(check())
    override val ascent: Int get() = TTF_GetFontAscent(check())
    override val descent: Int get() = TTF_GetFontDescent(check())
    override val familyName: String? get() = TTF_GetFontFamilyName(check())?.toKString()
    override val styleName: String? get() = TTF_GetFontStyleName(check())?.toKString()
    override val fixedWidth: Boolean get() = TTF_FontIsFixedWidth(check())
    override val scalable: Boolean get() = TTF_FontIsScalable(check())
    override val weight: Int get() = TTF_GetFontWeight(check())

    override fun hasGlyph(ch: Int): Boolean = TTF_FontHasGlyph(check(), ch.toUInt())

    override fun getGlyphMetrics(ch: Int): SDLTTFGlyphMetrics? = memScoped {
        val minx = alloc<IntVar>()
        val maxx = alloc<IntVar>()
        val miny = alloc<IntVar>()
        val maxy = alloc<IntVar>()
        val advance = alloc<IntVar>()
        if (!TTF_GetGlyphMetrics(check(), ch.toUInt(), minx.ptr, maxx.ptr, miny.ptr, maxy.ptr, advance.ptr)) {
            null
        } else {
            SDLTTFGlyphMetrics(minx.value, maxx.value, miny.value, maxy.value, advance.value)
        }
    }

    override fun getGlyphKerning(previousCh: Int, ch: Int): Int? = memScoped {
        val kerning = alloc<IntVar>()
        if (!TTF_GetGlyphKerning(check(), previousCh.toUInt(), ch.toUInt(), kerning.ptr)) {
            null
        } else {
            kerning.value
        }
    }

    override fun getStringSize(text: String): SDLPoint? = memScoped {
        val w = alloc<IntVar>()
        val h = alloc<IntVar>()
        if (!TTF_GetStringSize(check(), text, 0u, w.ptr, h.ptr)) {
            null
        } else {
            SDLPoint(w.value, h.value)
        }
    }

    override fun getStringSizeWrapped(text: String, wrapWidth: Int): SDLPoint? = memScoped {
        val w = alloc<IntVar>()
        val h = alloc<IntVar>()
        if (!TTF_GetStringSizeWrapped(check(), text, 0u, wrapWidth, w.ptr, h.ptr)) {
            null
        } else {
            SDLPoint(w.value, h.value)
        }
    }

    override fun measureString(text: String, maxWidth: Int): SDLTTFMeasure? = memScoped {
        val measuredWidth = alloc<IntVar>()
        val measuredLength = alloc<size_tVar>()
        if (!TTF_MeasureString(check(), text, 0u, maxWidth, measuredWidth.ptr, measuredLength.ptr)) {
            null
        } else {
            SDLTTFMeasure(measuredWidth.value, measuredLength.value.toInt())
        }
    }

    override fun addFallbackFont(fallback: SDLTTFFont): Boolean {
        val fallbackPtr = (fallback as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("fallback is not a native SDL_ttf font")
        return TTF_AddFallbackFont(check(), fallbackPtr)
    }

    override fun removeFallbackFont(fallback: SDLTTFFont) {
        val fallbackPtr = (fallback as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("fallback is not a native SDL_ttf font")
        TTF_RemoveFallbackFont(check(), fallbackPtr)
    }

    override fun clearFallbackFonts() {
        TTF_ClearFallbackFonts(check())
    }

    override fun close() {
        val f = font
        if (f == null) return
        font = null
        TTF_CloseFont(f)
    }
}

internal class NativeSDLTTFTextEngine internal constructor(
    ptr: CPointer<TTF_TextEngine>?,
    private val rendererEngine: Boolean,
) : SDLTTFTextEngine {

    internal var engine: CPointer<TTF_TextEngine>? = ptr

    override val ptr: Long get() = engine?.rawValue?.toLong() ?: 0L

    internal fun check(): CPointer<TTF_TextEngine> =
        engine ?: throw IllegalStateException("SDL_ttf text engine is closed")

    override fun close() {
        val e = engine
        if (e == null) return
        engine = null
        if (rendererEngine) {
            TTF_DestroyRendererTextEngine(e)
        } else {
            TTF_DestroySurfaceTextEngine(e)
        }
    }
}

internal class NativeSDLTTFText internal constructor(
    ptr: CPointer<TTF_Text>?,
    private val engine: NativeSDLTTFTextEngine?,
    private val font: NativeSDLTTFFont?,
) : SDLTTFText {

    internal var textPtr: CPointer<TTF_Text>? = ptr

    override val ptr: Long get() = textPtr?.rawValue?.toLong() ?: 0L

    internal fun check(): CPointer<TTF_Text> =
        textPtr ?: throw IllegalStateException("SDL_ttf text is closed")

    override val text: String?
        get() {
            val t = check().pointed
            return t.text?.toKString()
        }

    override val numLines: Int
        get() = check().pointed.num_lines

    override var color: SDLColor
        get() = memScoped {
            val r = alloc<Uint8Var>()
            val g = alloc<Uint8Var>()
            val b = alloc<Uint8Var>()
            val a = alloc<Uint8Var>()
            check(!TTF_GetTextColor(check(), r.ptr, g.ptr, b.ptr, a.ptr)) { "TTF_GetTextColor failed" }
            SDLColor(r.value.toInt(), g.value.toInt(), b.value.toInt(), a.value.toInt())
        }
        set(value) {
            TTF_SetTextColor(check(), value.r.toUByte(), value.g.toUByte(), value.b.toUByte(), value.a.toUByte())
        }

    override var position: SDLPoint?
        get() = memScoped {
            val x = alloc<IntVar>()
            val y = alloc<IntVar>()
            if (!TTF_GetTextPosition(check(), x.ptr, y.ptr)) {
                null
            } else {
                SDLPoint(x.value, y.value)
            }
        }
        set(value) {
            TTF_SetTextPosition(check(), value?.x ?: 0, value?.y ?: 0)
        }

    override var wrapWidth: Int
        get() = memScoped {
            val wrap = alloc<IntVar>()
            TTF_GetTextWrapWidth(check(), wrap.ptr)
            wrap.value
        }
        set(value) {
            TTF_SetTextWrapWidth(check(), value)
        }

    override var wrapWhitespaceVisible: Boolean
        get() = TTF_TextWrapWhitespaceVisible(check())
        set(value) {
            TTF_SetTextWrapWhitespaceVisible(check(), value)
        }

    override val size: SDLPoint?
        get() = memScoped {
            val w = alloc<IntVar>()
            val h = alloc<IntVar>()
            if (!TTF_GetTextSize(check(), w.ptr, h.ptr)) {
                null
            } else {
                SDLPoint(w.value, h.value)
            }
        }

    override fun draw(x: Float, y: Float): Boolean = TTF_DrawRendererText(check(), x, y)

    override fun drawOnSurface(x: Int, y: Int, surface: SDLSurface): Boolean {
        if (surface.ptr == 0L) throw IllegalStateException("SDL surface is closed")
        return TTF_DrawSurfaceText(check(), x, y, surface.ptr.toCPointer<SDL_Surface>())
    }

    override fun setText(text: String): Boolean = TTF_SetTextString(check(), text, 0u)

    override fun append(text: String): Boolean = TTF_AppendTextString(check(), text, 0u)

    override fun insert(offset: Int, text: String): Boolean = TTF_InsertTextString(check(), offset, text, 0u)

    override fun delete(offset: Int, length: Int): Boolean = TTF_DeleteTextString(check(), offset, length)

    override fun subStringAt(offset: Int): SDLTTFSubString? = memScoped {
        val sub = alloc<TTF_SubString>()
        if (!TTF_GetTextSubString(check(), offset, sub.ptr)) {
            null
        } else {
            sub.toSubString()
        }
    }

    override fun subStringForLine(line: Int): SDLTTFSubString? = memScoped {
        val sub = alloc<TTF_SubString>()
        if (!TTF_GetTextSubStringForLine(check(), line, sub.ptr)) {
            null
        } else {
            sub.toSubString()
        }
    }

    override fun subStringForPoint(x: Int, y: Int): SDLTTFSubString? = memScoped {
        val sub = alloc<TTF_SubString>()
        if (!TTF_GetTextSubStringForPoint(check(), x, y, sub.ptr)) {
            null
        } else {
            sub.toSubString()
        }
    }

    override fun close() {
        val t = textPtr
        if (t == null) return
        textPtr = null
        TTF_DestroyText(t)
    }
}

private fun TTF_SubString.toSubString(): SDLTTFSubString = SDLTTFSubString(
    flags = flags.toInt(),
    offset = offset,
    length = length,
    lineIndex = line_index,
    rect = SDLRect(rect.x, rect.y, rect.w, rect.h),
)

actual object SDLTTF {

    actual fun init(): Boolean = TTF_Init()

    actual fun quit() {
        TTF_Quit()
    }

    actual fun wasInit(): Int = TTF_WasInit()

    actual fun version(): Int = TTF_Version()

    actual fun getFreeTypeVersion(): SDLTTFVersion = memScoped {
        val major = alloc<IntVar>()
        val minor = alloc<IntVar>()
        val patch = alloc<IntVar>()
        TTF_GetFreeTypeVersion(major.ptr, minor.ptr, patch.ptr)
        SDLTTFVersion(major.value, minor.value, patch.value)
    }

    actual fun getHarfBuzzVersion(): SDLTTFVersion = memScoped {
        val major = alloc<IntVar>()
        val minor = alloc<IntVar>()
        val patch = alloc<IntVar>()
        TTF_GetHarfBuzzVersion(major.ptr, minor.ptr, patch.ptr)
        SDLTTFVersion(major.value, minor.value, patch.value)
    }

    actual fun error(): String? = SDL_GetError()?.toKString()?.takeIf { it.isNotEmpty() }

    actual fun clearError() {
        SDL_ClearError()
    }

    actual fun openFont(path: String, pointSize: Float): SDLTTFFont {
        val font = TTF_OpenFont(path, pointSize)
            ?: throw IllegalStateException("TTF_OpenFont failed: ${SDLTTF.error()}")
        return NativeSDLTTFFont(font)
    }

    actual fun openFontIO(stream: SDLIOStream, closeIO: Boolean, pointSize: Float): SDLTTFFont {
        if (stream.ptr == 0L) throw IllegalStateException("SDL IO stream is closed")
        val font = TTF_OpenFontIO(stream.ptr.toCPointer<SDL_IOStream>(), closeIO, pointSize)
            ?: throw IllegalStateException("TTF_OpenFontIO failed: ${SDLTTF.error()}")
        return NativeSDLTTFFont(font)
    }

    actual fun copyFont(existingFont: SDLTTFFont): SDLTTFFont {
        val existing = (existingFont as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("existingFont is not a native SDL_ttf font")
        val font = TTF_CopyFont(existing)
            ?: throw IllegalStateException("TTF_CopyFont failed: ${SDLTTF.error()}")
        return NativeSDLTTFFont(font)
    }

    actual fun createRendererTextEngine(renderer: SDLRenderer): SDLTTFTextEngine {
        if (renderer.ptr == 0L) throw IllegalStateException("SDL renderer is closed")
        val engine = TTF_CreateRendererTextEngine(renderer.ptr.toCPointer<SDL_Renderer>())
            ?: throw IllegalStateException("TTF_CreateRendererTextEngine failed: ${SDLTTF.error()}")
        return NativeSDLTTFTextEngine(engine, rendererEngine = true)
    }

    actual fun createSurfaceTextEngine(): SDLTTFTextEngine {
        val engine = TTF_CreateSurfaceTextEngine()
            ?: throw IllegalStateException("TTF_CreateSurfaceTextEngine failed: ${SDLTTF.error()}")
        return NativeSDLTTFTextEngine(engine, rendererEngine = false)
    }

    actual fun createText(engine: SDLTTFTextEngine?, font: SDLTTFFont?, text: String): SDLTTFText {
        val enginePtr = (engine as? NativeSDLTTFTextEngine)?.engine
        val fontPtr = (font as? NativeSDLTTFFont)?.font
        val t = TTF_CreateText(enginePtr, fontPtr, text, 0u)
            ?: throw IllegalStateException("TTF_CreateText failed: ${SDLTTF.error()}")
        return NativeSDLTTFText(t, engine as? NativeSDLTTFTextEngine, font as? NativeSDLTTFFont)
    }

    private fun surfaceOf(ptr: CPointer<SDL_Surface>?): SDLSurface? =
        (ptr?.rawValue?.toLong() ?: 0L).toSDLSurface(owned = true)

    actual fun renderTextSolid(font: SDLTTFFont, text: String, fg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a native SDL_ttf font")
        val surf = if (wrapWidth > 0) {
            TTF_RenderText_Solid_Wrapped(f, text, 0u, fg.toCValue(), wrapWidth)
        } else {
            TTF_RenderText_Solid(f, text, 0u, fg.toCValue())
        }
        return surfaceOf(surf)
    }

    actual fun renderTextShaded(font: SDLTTFFont, text: String, fg: SDLColor, bg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a native SDL_ttf font")
        val surf = if (wrapWidth > 0) {
            TTF_RenderText_Shaded_Wrapped(f, text, 0u, fg.toCValue(), bg.toCValue(), wrapWidth)
        } else {
            TTF_RenderText_Shaded(f, text, 0u, fg.toCValue(), bg.toCValue())
        }
        return surfaceOf(surf)
    }

    actual fun renderTextBlended(font: SDLTTFFont, text: String, fg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a native SDL_ttf font")
        val surf = if (wrapWidth > 0) {
            TTF_RenderText_Blended_Wrapped(f, text, 0u, fg.toCValue(), wrapWidth)
        } else {
            TTF_RenderText_Blended(f, text, 0u, fg.toCValue())
        }
        return surfaceOf(surf)
    }

    actual fun renderTextLCD(font: SDLTTFFont, text: String, fg: SDLColor, bg: SDLColor, wrapWidth: Int): SDLSurface? {
        val f = (font as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a native SDL_ttf font")
        val surf = if (wrapWidth > 0) {
            TTF_RenderText_LCD_Wrapped(f, text, 0u, fg.toCValue(), bg.toCValue(), wrapWidth)
        } else {
            TTF_RenderText_LCD(f, text, 0u, fg.toCValue(), bg.toCValue())
        }
        return surfaceOf(surf)
    }

    actual fun renderGlyphSolid(font: SDLTTFFont, ch: Int, fg: SDLColor): SDLSurface? {
        val f = (font as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a native SDL_ttf font")
        return surfaceOf(TTF_RenderGlyph_Solid(f, ch.toUInt(), fg.toCValue()))
    }

    actual fun renderGlyphShaded(font: SDLTTFFont, ch: Int, fg: SDLColor, bg: SDLColor): SDLSurface? {
        val f = (font as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a native SDL_ttf font")
        return surfaceOf(TTF_RenderGlyph_Shaded(f, ch.toUInt(), fg.toCValue(), bg.toCValue()))
    }

    actual fun renderGlyphBlended(font: SDLTTFFont, ch: Int, fg: SDLColor): SDLSurface? {
        val f = (font as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a native SDL_ttf font")
        return surfaceOf(TTF_RenderGlyph_Blended(f, ch.toUInt(), fg.toCValue()))
    }

    actual fun renderGlyphLCD(font: SDLTTFFont, ch: Int, fg: SDLColor, bg: SDLColor): SDLSurface? {
        val f = (font as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a native SDL_ttf font")
        return surfaceOf(TTF_RenderGlyph_LCD(f, ch.toUInt(), fg.toCValue(), bg.toCValue()))
    }

    actual fun getGlyphImage(font: SDLTTFFont, ch: Int): SDLSurface? {
        val f = (font as? NativeSDLTTFFont)?.check()
            ?: throw IllegalArgumentException("font is not a native SDL_ttf font")
        return surfaceOf(TTF_GetGlyphImage(f, ch.toUInt(), null))
    }
}
