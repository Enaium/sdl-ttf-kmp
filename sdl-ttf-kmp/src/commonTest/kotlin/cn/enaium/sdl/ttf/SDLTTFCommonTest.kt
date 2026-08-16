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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SDLTTFCommonTest {

    @Test
    fun initAndVersion() {
        assertTrue(SDLTTF.init(), "TTF_Init failed: ${SDLTTF.error()}")
        assertTrue(SDLTTF.wasInit() > 0)

        val version = SDLTTF.version()
        assertTrue(version >= 3000000, "expected SDL_ttf 3.x, got $version")

        val freeType = SDLTTF.getFreeTypeVersion()
        assertTrue(freeType.major >= 2, "unexpected FreeType version $freeType")

        SDLTTF.quit()
        assertEquals(0, SDLTTF.wasInit())
    }

    @Test
    fun openFontAndMetrics() {
        assertTrue(SDLTTF.init())
        val fontPath = resolveTestFont() ?: run {
            SDLTTF.quit()
            return
        }

        val font = SDLTTF.openFont(fontPath, 24f)
        assertTrue(font.height > 0, "height: ${font.height}")
        assertTrue(font.ascent > 0, "ascent: ${font.ascent}")
        assertNotNull(font.familyName, "familyName (font: $fontPath)")
        val helloSize = font.getStringSize("Hello")
        assertTrue(
            helloSize?.x ?: 0 > 0,
            "getStringSize(\"Hello\") failed: ${SDLTTF.error()} (font: $fontPath)",
        )
        assertNotNull(font.measureString("Hello", 100), "measureString: ${SDLTTF.error()}")
        assertTrue(
            font.hasGlyph('A'.code) || font.hasGlyph('文'.code),
            "hasGlyph failed: ${SDLTTF.error()} (font: $fontPath)",
        )
        font.close()

        SDLTTF.quit()
    }

    /** A font that exists on the test machine, or null to skip the test. */
    private fun resolveTestFont(): String? {
        val candidates = listOf(
            "/System/Library/Fonts/Hiragino Sans GB.ttc",
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/Helvetica.ttc",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/TTF/DejaVuSans.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/msyh.ttc",
        )
        for (path in candidates) {
            try {
                val font = SDLTTF.openFont(path, 12f)
                font.close()
                return path
            } catch (_: Throwable) {
                // keep looking
            }
        }
        return null
    }
}
