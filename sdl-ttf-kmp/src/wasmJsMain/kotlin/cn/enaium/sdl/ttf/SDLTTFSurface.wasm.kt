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
import cn.enaium.sdl.SDLRect
import cn.enaium.sdl.SDLSurface
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt32Array
import org.khronos.webgl.toInt8Array

/**
 * An [SDLSurface] backed by an SDL_Surface created by SDL_ttf inside the
 * shared Emscripten module. Surface operations use the sdl-kmp SDL3 shim
 * installed on the same module, so TTF surfaces can be blitted onto (and
 * from) surfaces created by sdl-kmp.
 */
internal class WasmTTFSurface(handle: Int, private val owned: Boolean) : SDLSurface {

    internal var surface: Int = handle

    override var ptr: Long
        get() = if (surface == 0) 0L else surface.toLong()
        set(value) {
            surface = value.toInt()
        }

    private fun check(): Int =
        surface.also { if (it == 0) throw IllegalStateException("SDL surface is closed") }

    override val width: Int get() = sdl_kmp_GetSurfaceWidth(check())
    override val height: Int get() = sdl_kmp_GetSurfaceHeight(check())
    override val format: Int get() = sdl_kmp_GetSurfaceFormat(check())
    override val colorspace: Int get() = sdl_kmp_GetSurfaceColorspace(check())
    override val pitch: Int get() = sdl_kmp_GetSurfacePitch(check())

    private var lockedPixels: ByteArray? = null

    override val pixels: ByteArray
        get() {
            // Match the JVM/native surfaces: reading is allowed without an
            // explicit lock (SDL_Surface.pixels is valid for non-RLE
            // surfaces). While locked, the cached copy is returned so writes
            // are flushed back by unlock().
            lockedPixels?.let { return it }
            val size = pitch * height
            val ptr = sdl_kmp_GetSurfacePixels(check())
            return if (size > 0 && ptr != 0) {
                sdlKmpHeapBytes(ptr, size).toByteArray()
            } else {
                ByteArray(0)
            }
        }

    override fun lock(): Boolean {
        if (sdl_kmp_LockSurface(check()) == 0) return false
        val size = pitch * height
        val data = sdl_kmp_GetSurfacePixels(check())
        lockedPixels = if (size > 0 && data != 0) {
            sdlKmpHeapBytes(data, size).toByteArray()
        } else {
            ByteArray(0)
        }
        return true
    }

    override fun unlock() {
        val data = lockedPixels
        if (data != null && data.isNotEmpty()) {
            val pixelsPtr = sdl_kmp_GetSurfacePixels(check())
            if (pixelsPtr != 0) sdlKmpSetHeapBytes(pixelsPtr, data.toInt8Array())
        }
        sdl_kmp_UnlockSurface(check())
        lockedPixels = null
    }

    override fun fillRect(rect: SDLRect?, color: SDLColor): Boolean {
        val px = SDL.mapRGBA(format, color.r, color.g, color.b, color.a)
        return if (rect == null) {
            sdl_kmp_FillSurfaceRect(check(), 0, 0, 0, 0, 0, px) != 0
        } else {
            sdl_kmp_FillSurfaceRect(check(), 1, rect.x, rect.y, rect.width, rect.height, px) != 0
        }
    }

    override fun fillRects(rects: List<SDLRect>, color: SDLColor): Boolean {
        if (rects.isEmpty()) return true
        val px = SDL.mapRGBA(format, color.r, color.g, color.b, color.a)
        val arr = IntArray(rects.size * 4).also { a ->
            rects.forEachIndexed { i, r ->
                a[i * 4] = r.x
                a[i * 4 + 1] = r.y
                a[i * 4 + 2] = r.width
                a[i * 4 + 3] = r.height
            }
        }
        return sdl_kmp_FillSurfaceRects(check(), arr.toInt32Array(), rects.size, px) != 0
    }

    override fun blit(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?): Boolean {
        if (dst.ptr == 0L) return false
        return sdl_kmp_BlitSurface(
            check(), if (src != null) 1 else 0, src?.x ?: 0, src?.y ?: 0, src?.width ?: 0, src?.height ?: 0,
            dst.ptr.toInt(), if (dstRect != null) 1 else 0, dstRect?.x ?: 0, dstRect?.y ?: 0, dstRect?.width ?: 0, dstRect?.height ?: 0,
        ) != 0
    }

    override fun blitScaled(src: SDLRect?, dst: SDLSurface, dstRect: SDLRect?, scaleMode: Int): Boolean {
        if (dst.ptr == 0L) return false
        return sdl_kmp_BlitSurfaceScaled(
            check(), if (src != null) 1 else 0, src?.x ?: 0, src?.y ?: 0, src?.width ?: 0, src?.height ?: 0,
            dst.ptr.toInt(), if (dstRect != null) 1 else 0, dstRect?.x ?: 0, dstRect?.y ?: 0, dstRect?.width ?: 0, dstRect?.height ?: 0,
            scaleMode,
        ) != 0
    }

    override fun saveBMP(path: String): Boolean = sdl_kmp_SaveBMP(check(), path) != 0

    override fun convert(format: Int): SDLSurface {
        val converted = sdl_kmp_ConvertSurface(check(), format)
        check(converted != 0) { "SDL_ConvertSurface failed: ${SDL.error()}" }
        return WasmTTFSurface(converted, owned = true)
    }

    override fun close() {
        if (surface != 0) {
            if (owned) sdl_kmp_DestroySurface(surface)
            surface = 0
        }
    }
}

internal actual fun Long.toSDLSurface(owned: Boolean): SDLSurface? =
    if (this == 0L) null else WasmTTFSurface(this.toInt(), owned)
