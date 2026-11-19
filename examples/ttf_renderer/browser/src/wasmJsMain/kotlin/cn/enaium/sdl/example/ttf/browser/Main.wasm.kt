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

package cn.enaium.sdl.example.ttf.browser

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.example.ttf.createTTFTextDemo
import cn.enaium.sdl.ttf.SDLTTF
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Browser (wasmJs) entry point.
 *
 * Browsers cannot repaint a busy main thread, so the shared [TTFTextDemo]
 * frame state machine (driven by native/JVM through a blocking loop) is run
 * here from `requestAnimationFrame` instead — one frame per browser paint.
 *
 * The host page initializes the combined SDL3 + SDL_ttf Emscripten module
 * (ttf_kmp_glue.js) and writes the demo font into its filesystem before this
 * module is imported, so the wasmJs actuals' JS globals and the font file are
 * already available when [main] runs.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() {
    SDL.setMainReady()

    if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
        // The dummy driver lets the same code run under Node (headless CI).
        SDL.setHint("SDL_VIDEO_DRIVER", "dummy")
        if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
            error("SDL_Init(VIDEO) failed: ${SDL.error()}")
        }
    }

    if (!SDLTTF.init()) {
        error("TTF_Init failed: ${SDLTTF.error()}")
    }

    println("SDL ${SDL.version()} (${SDL.revision()})")
    println("Video driver: ${SDL.getCurrentVideoDriver()}")

    // Headless (Node) runs have no quit event and no real paint loop; stop
    // after a fixed frame count.
    val headless = SDL.getCurrentVideoDriver() == "dummy"
    val maxFrames = if (headless) 5 else Int.MAX_VALUE

    val demo = createTTFTextDemo("/fonts/DejaVuSans.ttf", maxFrames) ?: run {
        SDLTTF.quit()
        SDL.quit()
        return
    }

    fun frame() {
        if (demo.frame()) {
            requestAnimationFrame { frame() }
        } else {
            println("ran ${demo.frameCount()} frames")
            demo.close()
            SDLTTF.quit()
            SDL.quit()
        }
    }

    requestAnimationFrame { frame() }
}

private external fun requestAnimationFrame(callback: () -> Unit): Int
