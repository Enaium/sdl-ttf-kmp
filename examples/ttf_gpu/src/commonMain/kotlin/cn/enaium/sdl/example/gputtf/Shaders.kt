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

package cn.enaium.sdl.example.gputtf

/**
 * Shaders for the GPU text demo.
 *
 * The vertex shader takes the per-vertex pixel position (location 0,
 * positive Y upwards, pre-transformed to NDC on the CPU) and texture
 * coordinates (location 1); the fragment shaders sample the glyph atlas.
 *
 * - TEXT_VERT_SPV / TEXT_FRAG_SPV / SDF_FRAG_SPV are precompiled SPIR-V
 *   (Vulkan/Android), compiled from shaders/text.{vert,frag} and
 *   shaders/sdf.frag with `glslangValidator --target-env vulkan1.0 -V`.
 * - The MSL variants are Metal Shading Language sources (macOS Metal
 *   backend), written by hand to mirror the GLSL semantics.
 */

internal val TEXT_VERT_SPV = byteArrayOf(
    3.toByte(), 2.toByte(), 35.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 8.toByte(), 0.toByte(),
    31.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 2.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    71.toByte(), 76.toByte(), 83.toByte(), 76.toByte(), 46.toByte(), 115.toByte(), 116.toByte(), 100.toByte(), 46.toByte(), 52.toByte(), 53.toByte(), 48.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 15.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 109.toByte(), 97.toByte(), 105.toByte(), 110.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    194.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    109.toByte(), 97.toByte(), 105.toByte(), 110.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 6.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 103.toByte(), 108.toByte(), 95.toByte(), 80.toByte(), 101.toByte(), 114.toByte(), 86.toByte(), 101.toByte(),
    114.toByte(), 116.toByte(), 101.toByte(), 120.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 6.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 103.toByte(), 108.toByte(), 95.toByte(), 80.toByte(),
    111.toByte(), 115.toByte(), 105.toByte(), 116.toByte(), 105.toByte(), 111.toByte(), 110.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 7.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 103.toByte(), 108.toByte(), 95.toByte(), 80.toByte(),
    111.toByte(), 105.toByte(), 110.toByte(), 116.toByte(), 83.toByte(), 105.toByte(), 122.toByte(), 101.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    103.toByte(), 108.toByte(), 95.toByte(), 67.toByte(), 108.toByte(), 105.toByte(), 112.toByte(), 68.toByte(), 105.toByte(), 115.toByte(), 116.toByte(), 97.toByte(),
    110.toByte(), 99.toByte(), 101.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 103.toByte(), 108.toByte(), 95.toByte(), 67.toByte(), 117.toByte(), 108.toByte(), 108.toByte(), 68.toByte(),
    105.toByte(), 115.toByte(), 116.toByte(), 97.toByte(), 110.toByte(), 99.toByte(), 101.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 112.toByte(), 111.toByte(), 115.toByte(), 105.toByte(), 116.toByte(), 105.toByte(), 111.toByte(), 110.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    118.toByte(), 85.toByte(), 86.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    117.toByte(), 118.toByte(), 0.toByte(), 0.toByte(), 71.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 72.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    72.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 72.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 72.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    19.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 33.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 22.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    21.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    30.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 21.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    14.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 15.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    20.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 21.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 128.toByte(), 63.toByte(),
    32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 25.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 27.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    27.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 54.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    248.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 61.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 22.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 80.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    24.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 22.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    20.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 21.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 65.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    25.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 26.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    15.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 62.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 26.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    24.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 61.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 62.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 253.toByte(), 0.toByte(), 1.toByte(), 0.toByte(),
    56.toByte(), 0.toByte(), 1.toByte(), 0.toByte(),
)

internal val TEXT_FRAG_SPV = byteArrayOf(
    3.toByte(), 2.toByte(), 35.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 8.toByte(), 0.toByte(),
    20.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 2.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    71.toByte(), 76.toByte(), 83.toByte(), 76.toByte(), 46.toByte(), 115.toByte(), 116.toByte(), 100.toByte(), 46.toByte(), 52.toByte(), 53.toByte(), 48.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 15.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 109.toByte(), 97.toByte(), 105.toByte(), 110.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 194.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 109.toByte(), 97.toByte(), 105.toByte(), 110.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    5.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 111.toByte(), 117.toByte(), 116.toByte(), 67.toByte(),
    111.toByte(), 108.toByte(), 111.toByte(), 114.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 97.toByte(), 116.toByte(), 108.toByte(), 97.toByte(), 115.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    5.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 118.toByte(), 85.toByte(), 86.toByte(), 0.toByte(),
    71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    33.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 34.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    33.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    22.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    23.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    25.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    27.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    15.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    15.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 54.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 248.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    61.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 61.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 15.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 87.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 62.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 253.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 56.toByte(), 0.toByte(), 1.toByte(), 0.toByte(),
)

internal val SDF_FRAG_SPV = byteArrayOf(
    3.toByte(), 2.toByte(), 35.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 8.toByte(), 0.toByte(),
    38.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 2.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    71.toByte(), 76.toByte(), 83.toByte(), 76.toByte(), 46.toByte(), 115.toByte(), 116.toByte(), 100.toByte(), 46.toByte(), 52.toByte(), 53.toByte(), 48.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 15.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 109.toByte(), 97.toByte(), 105.toByte(), 110.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 194.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 109.toByte(), 97.toByte(), 105.toByte(), 110.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    5.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 100.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    5.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 97.toByte(), 116.toByte(), 108.toByte(), 97.toByte(),
    115.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    118.toByte(), 85.toByte(), 86.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    97.toByte(), 108.toByte(), 112.toByte(), 104.toByte(), 97.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 111.toByte(), 117.toByte(), 116.toByte(), 67.toByte(), 111.toByte(), 108.toByte(), 111.toByte(), 114.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    33.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 34.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 71.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 2.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 33.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 22.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 25.toByte(), 0.toByte(), 9.toByte(), 0.toByte(),
    9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 27.toByte(), 0.toByte(), 3.toByte(), 0.toByte(),
    10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 9.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 11.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    15.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 14.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 15.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 21.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    20.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 20.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 21.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    24.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 102.toByte(), 102.toByte(), 230.toByte(), 62.toByte(), 43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 25.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 205.toByte(), 204.toByte(), 12.toByte(), 63.toByte(),
    32.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 28.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    43.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 31.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 128.toByte(), 63.toByte(), 44.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 30.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 31.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 31.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    31.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 54.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    4.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    248.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    59.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    7.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 61.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 10.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 61.toByte(), 0.toByte(), 4.toByte(), 0.toByte(),
    14.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 16.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    87.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    13.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 17.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 22.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 19.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    3.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 62.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    22.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 61.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    26.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 12.toByte(), 0.toByte(), 8.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 27.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    49.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 24.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 25.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    26.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 62.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    27.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 61.toByte(), 0.toByte(), 4.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    33.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 23.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(),
    6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 34.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    35.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    81.toByte(), 0.toByte(), 5.toByte(), 0.toByte(), 6.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 36.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    32.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 80.toByte(), 0.toByte(), 7.toByte(), 0.toByte(),
    18.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 37.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 34.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    35.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 36.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 33.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    62.toByte(), 0.toByte(), 3.toByte(), 0.toByte(), 29.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 37.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
    253.toByte(), 0.toByte(), 1.toByte(), 0.toByte(), 56.toByte(), 0.toByte(), 1.toByte(), 0.toByte(),
)

internal val TEXT_VERT_MSL = """
#include <metal_stdlib>
#include <simd/simd.h>

using namespace metal;

struct VSInput {
    float2 position [[attribute(0)]];
    float2 uv [[attribute(1)]];
};

struct VSOutput {
    float2 uv [[user(locn0)]];
    float4 position [[position]];
};

vertex VSOutput vs_main(VSInput in [[stage_in]]) {
    VSOutput out;
    out.uv = in.uv;
    out.position = float4(in.position, 0.0, 1.0);
    return out;
}
"""

internal val TEXT_FRAG_MSL = """
#include <metal_stdlib>
#include <simd/simd.h>

using namespace metal;

struct FSInput {
    float2 uv [[user(locn0)]];
};

fragment float4 fs_main(FSInput in [[stage_in]],
                        texture2d<float> atlas [[texture(0)]],
                        sampler atlasSampler [[sampler(0)]]) {
    return atlas.sample(atlasSampler, in.uv);
}
"""

internal val SDF_FRAG_MSL = """
#include <metal_stdlib>
#include <simd/simd.h>

using namespace metal;

struct FSInput {
    float2 uv [[user(locn0)]];
};

fragment float4 fs_main(FSInput in [[stage_in]],
                        texture2d<float> atlas [[texture(0)]],
                        sampler atlasSampler [[sampler(0)]]) {
    float d = atlas.sample(atlasSampler, in.uv).a;
    float alpha = smoothstep(0.45, 0.55, d);
    return float4(float3(1.0), alpha);
}
"""
