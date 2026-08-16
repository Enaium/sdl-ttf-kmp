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

package cn.enaium.sdl.example.ttf

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLColor
import cn.enaium.sdl.SDLFloatPoint
import cn.enaium.sdl.SDLFRect
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLIO
import cn.enaium.sdl.SDLKeycode
import cn.enaium.sdl.SDLPixelFormat
import cn.enaium.sdl.SDLPoint
import cn.enaium.sdl.SDLRect
import cn.enaium.sdl.SDLRenderer
import cn.enaium.sdl.SDLScaleMode
import cn.enaium.sdl.SDLSurface
import cn.enaium.sdl.SDLTexture
import cn.enaium.sdl.SDLTextureAccess
import cn.enaium.sdl.SDLWindow
import cn.enaium.sdl.SDLWindowEventType
import cn.enaium.sdl.SDLWindowFlags
import cn.enaium.sdl.SDLEvent
import cn.enaium.sdl.ttf.SDLTTF
import cn.enaium.sdl.ttf.SDLTTFDirection
import cn.enaium.sdl.ttf.SDLTTFFont
import cn.enaium.sdl.ttf.SDLTTFHorizontalAlignment
import cn.enaium.sdl.ttf.SDLTTFHinting
import cn.enaium.sdl.ttf.SDLTTFStyle
import cn.enaium.sdl.ttf.SDLTTFText
import cn.enaium.sdl.ttf.SDLTTFTextEngine

/**
 * The SDL_ttf demo as a frame state machine, shared by every platform.
 *
 * It exercises the sdl-ttf-kmp bindings on top of the sdl-kmp 2D renderer:
 *
 *  - text rendered to SDL surfaces ([SDLTTF.renderTextBlended] and friends),
 *    uploaded into renderer textures and drawn with [SDLRenderer.renderTexture];
 *  - the renderer text engine ([SDLTTF.createRendererTextEngine]) drawing
 *    text objects directly on the renderer via [SDLTTFText.draw];
 *  - the surface text engine ([SDLTTF.createSurfaceTextEngine]) drawing onto
 *    a software surface via [SDLTTFText.drawOnSurface], which is then
 *    presented as a texture;
 *  - fonts opened from files and from [SDLIOStream]s, font metrics, string
 *    measuring, glyph images, kerning and substrings.
 *
 * Text rendering is optimized for sharpness:
 *
 *  - [dpiScale] rasterizes text at the window's backing (physical) pixel
 *    size while drawing at logical size, so HiDPI displays get the full
 *    resolution (logical size != drawable size);
 *  - text textures are never upscaled: the rasterization size always matches
 *    the on-screen size;
 *  - [renderTextTexture] supports supersampling ([supersample] > 1): the text
 *    is rasterized Nx larger and drawn at logical size, which yields crisper
 *    edges than plain rasterization;
 *  - every text texture uses [SDLScaleMode.LINEAR] so any remaining scaling
 *    is smooth.
 */
class TTFTextDemo(
    private val window: SDLWindow,
    private val renderer: SDLRenderer,
    private val fontPath: String,
    private val maxFrames: Int,
) {
    /** Physical pixels per logical pixel (1.0 on non-HiDPI displays). */
    private val dpiScale: Float = window.sizeInPixels.y.toFloat() / window.size.y.toFloat()

    // ---- fonts ----
    private val font: SDLTTFFont = SDLTTF.openFont(fontPath, 32f)
    private val fontIO: SDLTTFFont
    // Dedicated fonts rasterized at the physical resolution for the HiDPI
    // canvas and the big glyph.
    private val canvasFont: SDLTTFFont = SDLTTF.openFont(fontPath, 32f * dpiScale)
    private val glyphFont: SDLTTFFont = SDLTTF.openFont(fontPath, 72f * dpiScale)

    // ---- engines ----
    private val rendererEngine: SDLTTFTextEngine = SDLTTF.createRendererTextEngine(renderer)
    private val surfaceEngine: SDLTTFTextEngine = SDLTTF.createSurfaceTextEngine()

    // ---- text objects ----
    private val title: SDLTTFText
    private val animated: SDLTTFText
    private val wrapped: SDLTTFText
    private val surfaceText: SDLTTFText

    // ---- pre-rendered surface textures ----
    private val titleTexture: SDLTexture
    private val blendedTexture: SDLTexture
    private val wrappedTexture: SDLTexture
    private val shadedTexture: SDLTexture
    private val lcdTexture: SDLTexture
    private val supersampledTexture: SDLTexture
    private val glyphTexture: SDLTexture

    // ---- surface text engine canvas (physical resolution) ----
    private val canvasWidth = (360f * dpiScale).toInt()
    private val canvasHeight = (140f * dpiScale).toInt()
    private val canvas: SDLSurface
    private val canvasTexture: SDLTexture

    private val start = SDL.getTicks()
    private var frames = 0
    private var running = true
    private var wrapEnabled = true
    private var wrapWhitespaceVisible = false
    private var fontGrow = true
    private var colorIndex = 0
    private var angle = 0.0
    private var bounceX = 60f
    private var bounceY = 60f
    private var vx = 2.5f
    private var vy = 1.8f

    private val palette = listOf(
        SDLColor(255, 200, 60),
        SDLColor(80, 255, 160),
        SDLColor(120, 180, 255),
        SDLColor(255, 120, 200),
    )

    init {
        // A second font opened from an SDL_IOStream (sdl-kmp's SDLIO API).
        // A file-backed stream is used: SDL's memory streams do not copy the
        // buffer, and sdl-kmp's native fromMem/fromConstMem hand out a
        // memScoped allocation that is freed on return. closeIO = true lets
        // SDL_ttf close the stream when the font is closed.
        val stream = SDLIO.openFile(fontPath, "rb")
            ?: error("failed to open font stream: ${SDL.error()}")
        fontIO = SDLTTF.openFontIO(stream, closeIO = true, pointSize = 32f)

        title = SDLTTF.createText(rendererEngine, font, "sdl-ttf-kmp")
        title.color = SDLColor(255, 255, 255)

        animated = SDLTTF.createText(rendererEngine, font, "Kotlin Multiplatform SDL_ttf bindings")

        wrapped = SDLTTF.createText(rendererEngine, fontIO, wrappedText(32f))
        wrapped.wrapWidth = 300

        surfaceText = SDLTTF.createText(
            surfaceEngine,
            canvasFont,
            "Surface text engine\nrenders onto a pixel buffer,\nthen becomes a texture",
        )
        surfaceText.wrapWidth = (320f * dpiScale).toInt()

        // ---- pre-render quality samples to surfaces -> textures ----
        titleTexture = renderTextTexture("sdl-ttf-kmp", 48f, SDLColor(255, 255, 255))
        blendedTexture = renderTextTexture("Blended: alpha antialiasing", 30f, SDLColor(240, 240, 240))
        wrappedTexture = renderTextTexture(
            "Wrapped blended text: this paragraph wraps at 300px and is uploaded as one texture.",
            20f,
            SDLColor(255, 180, 90),
            wrapWidth = 300,
        )
        shadedTexture = renderTextTexture(
            "Shaded: solid background",
            24f,
            SDLColor(30, 200, 120),
            bg = SDLColor(20, 40, 30),
        )
        lcdTexture = renderTextTexture(
            "LCD subpixel rendering",
            24f,
            SDLColor(120, 200, 255),
            bg = SDLColor(10, 15, 25),
            lcd = true,
        )
        // Supersampling: rasterized 4x larger, drawn at logical size. On
        // non-HiDPI displays this still looks noticeably sharper.
        supersampledTexture = renderTextTexture(
            "Supersampled: 4x rasterization, drawn at logical size",
            24f,
            SDLColor(200, 160, 255),
            supersample = 4f,
        )
        // The CJK glyph rendered with LCD subpixel rendering at a dedicated
        // point size (never upscaled); the background color is baked into the
        // surface (match the scene's clear color).
        val glyphSurface = SDLTTF.renderGlyphLCD(glyphFont, '文'.code, SDLColor(255, 120, 160), SDLColor(18, 18, 24))
        glyphTexture = surfaceToTexture(glyphSurface)

        canvas = SDL.createSurface(canvasWidth, canvasHeight, SDLPixelFormat.ARGB8888)
        canvasTexture = renderer.createTexture(
            format = canvas.format,
            access = SDLTextureAccess.STATIC,
            width = canvas.width,
            height = canvas.height,
        )

        // ---- console: font + library information ----
        println("SDL_ttf ${SDLTTF.version()}  FreeType ${SDLTTF.getFreeTypeVersion()}  HarfBuzz ${SDLTTF.getHarfBuzzVersion()}")
        println(
            "window: ${window.size} logical, ${window.sizeInPixels} physical " +
                "(dpiScale=${dpiScale})",
        )
        println("font: ${font.familyName} / ${font.styleName}  size=${font.size}  weight=${font.weight}  height=${font.height}  ascent=${font.ascent}  descent=${font.descent}")
        println("font: hinting=${font.hinting} kerning=${font.kerning} fixedWidth=${font.fixedWidth} scalable=${font.scalable} lineSkip=${font.lineSkip}")
        println("fontIO: ${fontIO.familyName} (opened from an SDL_IOStream)")

        val hello = "Hello, SDL_ttf!"
        font.getStringSize(hello)?.let { println("getStringSize(\"$hello\") = $it") }
        font.getStringSizeWrapped(hello, 100)?.let { println("getStringSizeWrapped(\"$hello\", 100) = $it") }
        font.measureString(hello, 200)?.let { println("measureString(\"$hello\", 200) = $it") }
        font.getGlyphMetrics('A'.code)?.let { println("glyph metrics 'A' = $it") }
        font.getGlyphKerning('A'.code, 'V'.code)?.let { println("kerning A/V = $it px") }
        println("hasGlyph('文') = ${font.hasGlyph('文'.code)}")

        font.style = SDLTTFStyle.BOLD or SDLTTFStyle.ITALIC
        println("bold+italic style string size: ${font.getStringSize(hello)}")
        font.style = SDLTTFStyle.NORMAL

        font.direction = SDLTTFDirection.LTR
        font.wrapAlignment = SDLTTFHorizontalAlignment.LEFT
        font.hinting = SDLTTFHinting.LIGHT_SUBPIXEL
    }

    private fun wrappedText(size: Float): String =
        "Wrapped renderer-engine text at ${size.toInt()}pt — resize with UP/DOWN. " +
            "SPACE toggles wrapping, W toggles whitespace visibility, C cycles the color, " +
            "click to inspect substrings."

    private fun surfaceToTexture(surface: SDLSurface?): SDLTexture {
        checkNotNull(surface) { "text rendering failed: ${SDLTTF.error()}" }
        val texture = renderer.createTexture(
            format = surface.format,
            access = SDLTextureAccess.STATIC,
            width = surface.width,
            height = surface.height,
        )
        texture.update(null, surface.pixels, surface.pitch)
        // Text textures are downscaled (HiDPI/supersampling); linear
        // filtering keeps that smooth.
        texture.scaleMode = SDLScaleMode.LINEAR
        surface.close()
        return texture
    }

    /**
     * Renders [text] into a texture.
     *
     * The text is rasterized at `pointSize * dpiScale * supersample` physical
     * pixels and drawn at `pointSize` logical pixels via [logicalRect], so it
     * is never upscaled: HiDPI displays get full resolution, and
     * [supersample] > 1 additionally downscales a larger rasterization for
     * crisper edges.
     */
    private fun renderTextTexture(
        text: String,
        pointSize: Float,
        color: SDLColor,
        bg: SDLColor? = null,
        wrapWidth: Int = 0,
        lcd: Boolean = false,
        supersample: Float = 1f,
    ): SDLTexture {
        val rasterScale = dpiScale * supersample
        val f = SDLTTF.openFont(fontPath, pointSize * rasterScale)
        val rasterWrap = (wrapWidth * rasterScale).toInt()
        val surface = when {
            lcd -> SDLTTF.renderTextLCD(f, text, color, bg ?: SDLColor(0, 0, 0), rasterWrap)
            bg != null -> SDLTTF.renderTextShaded(f, text, color, bg, rasterWrap)
            else -> SDLTTF.renderTextBlended(f, text, color, rasterWrap)
        }
        val texture = surfaceToTexture(surface)
        f.close()
        return texture
    }

    /** The logical-size destination rect for [texture] (rasterized at [dpiScale]). */
    private fun logicalRect(x: Float, y: Float, texture: SDLTexture): SDLFRect =
        SDLFRect(x, y, texture.size.x / dpiScale, texture.size.y / dpiScale)

    /** The logical height of [texture] in pixels. */
    private fun logicalHeight(texture: SDLTexture): Int = (texture.size.y / dpiScale).toInt()

    /** The number of frames rendered so far. */
    fun frameCount(): Int = frames

    /** Runs one frame; returns `false` when the demo should stop. */
    fun frame(): Boolean {
        if (!running) return false

        // ---- events ----
        while (true) {
            val event = SDL.pollEvent() ?: break
            when (event) {
                is SDLEvent.Quit -> running = false
                is SDLEvent.Window ->
                    if (event.type == SDLWindowEventType.CLOSE_REQUESTED) {
                        running = false
                    }
                is SDLEvent.Key -> when {
                    !event.down -> Unit
                    event.keycode == SDLKeycode.ESCAPE -> running = false
                    event.keycode == SDLKeycode.SPACE -> {
                        wrapEnabled = !wrapEnabled
                        wrapped.wrapWidth = if (wrapEnabled) 300 else 0
                        println("wrap ${if (wrapEnabled) "enabled (300px)" else "disabled"}")
                    }
                    event.keycode == SDLKeycode.W -> {
                        wrapWhitespaceVisible = !wrapWhitespaceVisible
                        wrapped.wrapWhitespaceVisible = wrapWhitespaceVisible
                        println("wrap whitespace ${if (wrapWhitespaceVisible) "visible" else "hidden"}")
                    }
                    event.keycode == SDLKeycode.C -> {
                        colorIndex = (colorIndex + 1) % palette.size
                        println("animated color -> ${palette[colorIndex]}")
                    }
                    event.keycode == SDLKeycode.UP -> {
                        font.size = (font.size + 4f).coerceAtMost(96f)
                        println("font size -> ${font.size.toInt()}pt")
                    }
                    event.keycode == SDLKeycode.DOWN -> {
                        font.size = (font.size - 4f).coerceAtLeast(8f)
                        println("font size -> ${font.size.toInt()}pt")
                    }
                }
                is SDLEvent.MouseButton -> {
                    // Show the substring under the cursor (the wrapped text is
                    // drawn at (48, 398) with a 300px wrap).
                    val size = wrapped.size
                    if (size != null) {
                        val localX = event.x.toInt() - 48
                        val localY = event.y.toInt() - 398
                        if (localX in 0..size.x && localY in 0..size.y) {
                            val sub = wrapped.subStringForPoint(localX, localY)
                            if (sub != null) {
                                val text = wrapped.text ?: ""
                                val snippet = text.substring(
                                    sub.offset,
                                    (sub.offset + sub.length).coerceAtMost(text.length),
                                )
                                println("clicked substring (line ${sub.lineIndex}): \"$snippet\"  $sub")
                            }
                        }
                    }
                }
                else -> Unit
            }
        }

        // ---- update ----
        angle = (angle + 1.2) % 360.0
        bounceX += vx
        bounceY += vy
        val w = window.size.x
        val h = window.size.y
        if (bounceX < 0 || bounceX > w - 320) vx = -vx
        if (bounceY < 0 || bounceY > h - 60) vy = -vy
        bounceX = bounceX.coerceIn(0f, (w - 320).coerceAtLeast(0).toFloat())
        bounceY = bounceY.coerceIn(0f, (h - 60).coerceAtLeast(0).toFloat())

        animated.color = palette[colorIndex]

        // Grow/shrink the font size of the animated text periodically; text
        // objects using the font re-layout automatically.
        if (frames % 240 == 0) fontGrow = !fontGrow
        val target = if (fontGrow) 44f else 28f
        if (font.size != target) {
            font.size = (font.size + if (font.size < target) 0.4f else -0.4f).coerceIn(8f, 96f)
        }

        // ---- render ----
        renderer.drawColor = SDLColor(18, 18, 24)
        renderer.clear()

        // Title: rotated texture, drawn at its logical (1:1) size around its
        // own center — no upscaling.
        val titleW = titleTexture.size.x / dpiScale
        val titleH = titleTexture.size.y / dpiScale
        renderer.renderTextureRotated(
            texture = titleTexture,
            dst = SDLFRect(w / 2f - titleW / 2, 16f, titleW, titleH),
            angle = angle,
            center = SDLFloatPoint(titleW / 2, titleH / 2),
        )

        // Pre-rendered quality samples (logical sizes).
        renderer.renderTexture(blendedTexture, dst = logicalRect(40f, 100f, blendedTexture))
        renderer.renderTexture(wrappedTexture, dst = logicalRect(40f, 150f, wrappedTexture))

        // Shaded + LCD samples with a border.
        renderer.drawColor = SDLColor(70, 70, 90)
        renderer.drawRect(SDLRect(40, 250, 360, logicalHeight(shadedTexture) + 10))
        renderer.drawRect(SDLRect(420, 250, 340, logicalHeight(lcdTexture) + 10))
        renderer.renderTexture(shadedTexture, dst = SDLFRect(48f, 255f, shadedTexture.size.x / dpiScale, shadedTexture.size.y / dpiScale))
        renderer.renderTexture(lcdTexture, dst = SDLFRect(428f, 255f, lcdTexture.size.x / dpiScale, lcdTexture.size.y / dpiScale))

        // Supersampling sample with a border.
        renderer.drawColor = SDLColor(70, 70, 90)
        renderer.drawRect(SDLRect(40, 330, 460, logicalHeight(supersampledTexture) + 10))
        renderer.renderTexture(supersampledTexture, dst = SDLFRect(48f, 335f, supersampledTexture.size.x / dpiScale, supersampledTexture.size.y / dpiScale))

        // The CJK glyph rasterized at its display size (no upscaling).
        renderer.renderTexture(glyphTexture, dst = logicalRect(560f, 100f, glyphTexture))

        // Renderer-engine wrapped text (live, re-lays out on font size changes).
        renderer.drawColor = SDLColor(80, 80, 110)
        renderer.drawRect(SDLRect(40, 390, 300, 190))
        wrapped.draw(48f, 398f)

        // Surface-engine text: drawn on a physical-resolution software
        // surface every frame, then uploaded into the canvas texture and
        // displayed at logical size (HiDPI-correct).
        canvas.fillRect(null, SDLColor(30, 30, 60))
        surfaceText.color = SDLColor(160, 220, 255)
        surfaceText.drawOnSurface((8f * dpiScale).toInt(), (8f * dpiScale).toInt(), canvas)
        canvasTexture.update(null, canvas.pixels, canvas.pitch)
        renderer.renderTexture(canvasTexture, dst = SDLFRect(420f, 400f, 360f, 140f))

        // Bouncing animated text (renderer engine).
        animated.draw(bounceX, bounceY)

        renderer.drawColor = SDLColor(128, 128, 140)
        renderer.drawRect(SDLRect(0, 0, w - 1, h - 1))

        renderer.present()

        frames++
        if (frames % 120 == 0) {
            val elapsedMs = (SDL.getTicks() - start).toFloat() / 1000f
            println(
                "fps: ${(frames / elapsedMs).toInt()}  font=${font.size.toInt()}pt  " +
                    "wrap=${wrapped.wrapWidth}px  lines=${wrapped.numLines}",
            )
        }

        if (frames >= maxFrames) {
            running = false
        }
        return running
    }

    /** Releases all demo resources, including the window and renderer. */
    fun close() {
        titleTexture.close()
        blendedTexture.close()
        wrappedTexture.close()
        shadedTexture.close()
        lcdTexture.close()
        supersampledTexture.close()
        glyphTexture.close()
        canvasTexture.close()
        title.close()
        animated.close()
        wrapped.close()
        surfaceText.close()
        surfaceEngine.close()
        rendererEngine.close()
        // fontIO closes its stream (closeIO = true), so ioStream is not
        // closed separately.
        fontIO.close()
        canvasFont.close()
        glyphFont.close()
        font.close()
        canvas.close()
        renderer.close()
        window.close()
    }
}

/**
 * Sets up the demo: SDL init, window, renderer and fonts. Returns null when
 * window or renderer creation fails.
 */
fun createTTFTextDemo(fontPath: String, maxFrames: Int): TTFTextDemo? {
    val window = try {
        SDL.createWindow(
            title = "sdl-ttf-kmp example",
            width = 800,
            height = 600,
            flags = SDLWindowFlags.RESIZABLE,
        )
    } catch (t: Throwable) {
        println("window creation failed: ${t.message}")
        return null
    }

    val renderer = try {
        SDL.createRenderer(window)
    } catch (t: Throwable) {
        println("renderer creation failed: ${t.message}")
        window.close()
        return null
    }

    return try {
        TTFTextDemo(window, renderer, fontPath, maxFrames)
    } catch (t: Throwable) {
        println("demo setup failed: ${t.message}")
        if (t is IllegalStateException) println("SDL_ttf error: ${SDLTTF.error()}")
        renderer.close()
        window.close()
        null
    }
}

/**
 * Blocking runner used by native / JVM: drives [TTFTextDemo.frame] with
 * `SDL.delay` for frame pacing.
 */
fun runExample(fontPath: String?) {
    SDL.setMainReady()

    // Try video init with the platform's best driver. On headless CI or
    // servers the dummy driver is needed; on a desktop the native driver
    // (cocoa/x11) should work.
    var headless = false
    if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
        SDL.setHint("SDL_VIDEO_DRIVER", "dummy")
        if (SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
            println("video init fell back to the dummy driver — running headless")
            headless = true
        } else {
            error("SDL_Init(VIDEO) failed: ${SDL.error()}\nMake sure a display is available, or set SDL_VIDEO_DRIVER=dummy to run headless.")
        }
    }

    println("SDL ${SDL.version()} (${SDL.revision()})")
    println("Video driver: ${SDL.getCurrentVideoDriver()}")

    headless = headless || SDL.getCurrentVideoDriver() == "dummy"
    val maxFrames = if (headless) 300 else Int.MAX_VALUE

    if (!SDLTTF.init()) {
        error("TTF_Init failed: ${SDLTTF.error()}")
    }

    // Resolve a usable font: CLI argument, then well-known system fonts.
    val path = resolveFont(fontPath)
    println("font: $path")

    val demo = createTTFTextDemo(path, maxFrames) ?: run {
        SDLTTF.quit()
        SDL.quit()
        return
    }

    while (demo.frame()) {
        // ---- frame pacing (~60 FPS) ----
        SDL.delay(16)
    }

    println("ran ${demo.frameCount()} frames")
    demo.close()
    SDLTTF.quit()
    SDL.quit()
    if (headless) {
        println("headless run finished")
    }
}

/**
 * Finds a usable font file by trying [arg] first, then a Chinese-capable
 * system font (the demo renders the CJK glyph '文'), then other common
 * system fonts.
 */
fun resolveFont(arg: String?): String {
    // Common CJK-capable fonts. TTF_OpenFont opens the first face of a .ttc
    // collection, which is fine for these.
    val chineseCandidates = listOf(
        // macOS
        "/System/Library/Fonts/PingFang.ttc",
        "/System/Library/Fonts/Hiragino Sans GB.ttc",
        "/System/Library/Fonts/STHeiti Light.ttc",
        "/System/Library/Fonts/Supplemental/Songti.ttc",
        // Linux: Noto CJK, WenQuanYi, Arphic
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
        "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
        "/usr/share/fonts/truetype/arphic/uming.ttc",
        // Windows: 微软雅黑 / 黑体 / 宋体
        "C:/Windows/Fonts/msyh.ttc",
        "C:/Windows/Fonts/simhei.ttf",
        "C:/Windows/Fonts/simsun.ttc",
    )

    // Fonts without CJK support, used when no Chinese font is available.
    val fallbackCandidates = listOf(
        "fonts/DejaVuSans.ttf",
        "./fonts/DejaVuSans.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/TTF/DejaVuSans.ttf",
        "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/segoeui.ttf",
    )

    // An explicit argument wins, but only when it is actually readable;
    // otherwise fall through to the automatic candidates.
    arg?.takeIf { it.isNotBlank() }?.let { explicit ->
        val font = probeFont(explicit)
        if (font != null) {
            font.close()
            return explicit
        }
    }

    // Prefer a font that can actually render Chinese. TTF_OpenFont fails
    // (and sets SDL's error) when the file is missing or unreadable; the
    // probe discards the error.
    for (path in chineseCandidates) {
        val font = probeFont(path) ?: continue
        val hasCjk = font.hasGlyph('文'.code)
        font.close()
        if (hasCjk) {
            return path
        }
    }

    for (path in fallbackCandidates) {
        if (probeFont(path) != null) {
            return path
        }
    }
    error("no usable font found, tried: $fallbackCandidates and $chineseCandidates")
}

/** Opens [path] and returns the font if it is readable, or null otherwise. */
private fun probeFont(path: String): SDLTTFFont? = try {
    SDLTTF.openFont(path, 12f)
} catch (_: Throwable) {
    null
}
