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

import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLColor
import cn.enaium.sdl.SDLGPU
import cn.enaium.sdl.SDLGPUBuffer
import cn.enaium.sdl.SDLGPUBufferCreateInfo
import cn.enaium.sdl.SDLGPUBufferUsage
import cn.enaium.sdl.SDLGPUColorTargetDescription
import cn.enaium.sdl.SDLGPUColorTargetInfo
import cn.enaium.sdl.SDLGPUDevice
import cn.enaium.sdl.SDLGPUFilter
import cn.enaium.sdl.SDLGPUGraphicsPipeline
import cn.enaium.sdl.SDLGPUGraphicsPipelineCreateInfo
import cn.enaium.sdl.SDLGPUIndexElementSize
import cn.enaium.sdl.SDLGPULoadOp
import cn.enaium.sdl.SDLGPUPrimitiveType
import cn.enaium.sdl.SDLGPURenderPass
import cn.enaium.sdl.SDLGPURasterizerState
import cn.enaium.sdl.SDLGPUSampler
import cn.enaium.sdl.SDLGPUSamplerCreateInfo
import cn.enaium.sdl.SDLGPUTexture
import cn.enaium.sdl.SDLGPUShader
import cn.enaium.sdl.SDLGPUShaderFormat
import cn.enaium.sdl.SDLGPUShaderStage
import cn.enaium.sdl.SDLGPUStoreOp
import cn.enaium.sdl.SDLGPUTextureFormat
import cn.enaium.sdl.SDLGPUVertexAttribute
import cn.enaium.sdl.SDLGPUVertexBufferDescription
import cn.enaium.sdl.SDLGPUVertexElementFormat
import cn.enaium.sdl.SDLGPUVertexInputRate
import cn.enaium.sdl.SDLGPUVertexInputState
import cn.enaium.sdl.SDLGPUViewport
import cn.enaium.sdl.SDLGPUWindowTexture
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLKeycode
import cn.enaium.sdl.SDLWindow
import cn.enaium.sdl.SDLWindowEventType
import cn.enaium.sdl.SDLWindowFlags
import cn.enaium.sdl.SDLEvent
import cn.enaium.sdl.ttf.SDLTTF
import cn.enaium.sdl.ttf.SDLTTFFont
import cn.enaium.sdl.ttf.SDLTTFImageType
import cn.enaium.sdl.ttf.SDLTTFText
import cn.enaium.sdl.ttf.SDLTTFTextEngine

/** Vertex stride: float2 position + float2 uv. */
private const val VERTEX_STRIDE = 16
private const val MAX_VERTICES = 8192
private const val MAX_INDICES = 16384

/**
 * The SDL_ttf GPU demo: lays text out with the SDL_ttf GPU text engine and
 * renders it with the SDL3 GPU API (sdl-kmp's SDLGPU bindings), entirely
 * from commonMain.
 *
 * Per frame the text's draw sequences ([SDLTTFText.getGPUDrawData]) are
 * fetched: vertex positions in pixels (positive Y upwards), normalized UVs
 * into the glyph atlas and indices. The positions are transformed to NDC on
 * the CPU, the interleaved vertices and indices are uploaded into buffers,
 * the atlas texture is bound and each sequence is drawn with
 * `drawIndexedPrimitives`. ALPHA/COLOR sequences use the text pipeline,
 * SDF sequences (the signed distance in the atlas alpha) use the SDF
 * pipeline whose shader smoothsteps the distance.
 */
class GpuTextDemo(
    private val window: SDLWindow,
    private val device: SDLGPUDevice,
    private val fontPath: String,
) {
    private val font: SDLTTFFont = SDLTTF.openFont(fontPath, 48f)
    private val engine: SDLTTFTextEngine = SDLTTF.createGPUTextEngine(device)

    private val text: SDLTTFText = SDLTTF.createText(engine, font, "SDL_ttf GPU text engine")
    private val colored: SDLTTFText = SDLTTF.createText(engine, font, "GPU API - rendered from commonMain")
    private val sdfFont: SDLTTFFont = SDLTTF.openFont(fontPath, 64f).also { it.SDF = true }
    private val sdfText: SDLTTFText = SDLTTF.createText(engine, sdfFont, "SDF: sharp at any scale")

    private val textPipeline: SDLGPUGraphicsPipeline
    private val sdfPipeline: SDLGPUGraphicsPipeline
    private val sampler: SDLGPUSampler
    private val vertexBuffer: SDLGPUBuffer
    private val indexBuffer: SDLGPUBuffer

    private var angle = 0.0
    private var running = true

    init {
        colored.color = SDLColor(180, 200, 255)
        sdfText.color = SDLColor(255, 255, 255)

        val windowFormat = device.getWindowFormat(window)
        check(windowFormat != null) { "SDL_GetGPUSwapchainTextureFormat failed: ${SDL.error()}" }
        println("swapchain format: $windowFormat")

        // Shader format: Metal on macOS, SPIR-V elsewhere.
        val useMsl = (device.shaderFormats and SDLGPUShaderFormat.MSL) != 0
        check(useMsl || (device.shaderFormats and SDLGPUShaderFormat.SPIRV) != 0) {
            "device supports neither MSL nor SPIR-V (formats=0x${device.shaderFormats.toString(16)})"
        }

        val vertexShader: SDLGPUShader? = device.createShader(
            code = if (useMsl) TEXT_VERT_MSL.encodeToByteArray() else TEXT_VERT_SPV,
            format = if (useMsl) SDLGPUShaderFormat.MSL else SDLGPUShaderFormat.SPIRV,
            stage = SDLGPUShaderStage.VERTEX,
            entryPoint = if (useMsl) "vs_main" else "main",
            numSamplers = 0, numStorageTextures = 0, numStorageBuffers = 0, numUniformBuffers = 0,
        )
        val textFragment: SDLGPUShader? = device.createShader(
            code = if (useMsl) TEXT_FRAG_MSL.encodeToByteArray() else TEXT_FRAG_SPV,
            format = if (useMsl) SDLGPUShaderFormat.MSL else SDLGPUShaderFormat.SPIRV,
            stage = SDLGPUShaderStage.FRAGMENT,
            entryPoint = if (useMsl) "fs_main" else "main",
            numSamplers = 1, numStorageTextures = 0, numStorageBuffers = 0, numUniformBuffers = 0,
        )
        val sdfFragment: SDLGPUShader? = device.createShader(
            code = if (useMsl) SDF_FRAG_MSL.encodeToByteArray() else SDF_FRAG_SPV,
            format = if (useMsl) SDLGPUShaderFormat.MSL else SDLGPUShaderFormat.SPIRV,
            stage = SDLGPUShaderStage.FRAGMENT,
            entryPoint = if (useMsl) "fs_main" else "main",
            numSamplers = 1, numStorageTextures = 0, numStorageBuffers = 0, numUniformBuffers = 0,
        )
        check(vertexShader != null && textFragment != null && sdfFragment != null) {
            "shader creation failed: ${SDL.error()}"
        }

        val vertexInputState = SDLGPUVertexInputState(
            vertexBufferDescriptions = listOf(
                SDLGPUVertexBufferDescription(slot = 0, pitch = VERTEX_STRIDE, inputRate = SDLGPUVertexInputRate.VERTEX),
            ),
            vertexAttributes = listOf(
                SDLGPUVertexAttribute(location = 0, bufferSlot = 0, format = SDLGPUVertexElementFormat.FLOAT2, offset = 0),
                SDLGPUVertexAttribute(location = 1, bufferSlot = 0, format = SDLGPUVertexElementFormat.FLOAT2, offset = 8),
            ),
        )

        textPipeline = device.createGraphicsPipeline(
            SDLGPUGraphicsPipelineCreateInfo(
                vertexShader = vertexShader,
                fragmentShader = textFragment,
                vertexInputState = vertexInputState,
                primitiveType = SDLGPUPrimitiveType.TRIANGLELIST,
                rasterizerState = SDLGPURasterizerState(),
                targetDescriptions = listOf(SDLGPUColorTargetDescription(format = windowFormat)),
            ),
        ) ?: error("text pipeline creation failed: ${SDL.error()}")

        sdfPipeline = device.createGraphicsPipeline(
            SDLGPUGraphicsPipelineCreateInfo(
                vertexShader = vertexShader,
                fragmentShader = sdfFragment,
                vertexInputState = vertexInputState,
                primitiveType = SDLGPUPrimitiveType.TRIANGLELIST,
                rasterizerState = SDLGPURasterizerState(),
                targetDescriptions = listOf(SDLGPUColorTargetDescription(format = windowFormat)),
            ),
        ) ?: error("SDF pipeline creation failed: ${SDL.error()}")

        sampler = device.createSampler(
            SDLGPUSamplerCreateInfo(
                minFilter = SDLGPUFilter.LINEAR,
                magFilter = SDLGPUFilter.LINEAR,
            ),
        ) ?: error("sampler creation failed: ${SDL.error()}")

        vertexBuffer = device.createBuffer(
            SDLGPUBufferCreateInfo(usage = SDLGPUBufferUsage.VERTEX, size = MAX_VERTICES * VERTEX_STRIDE),
        ) ?: error("vertex buffer creation failed: ${SDL.error()}")
        indexBuffer = device.createBuffer(
            SDLGPUBufferCreateInfo(usage = SDLGPUBufferUsage.INDEX, size = MAX_INDICES * 2),
        ) ?: error("index buffer creation failed: ${SDL.error()}")

        vertexShader.close()
        textFragment.close()
        sdfFragment.close()

        println("font: ${font.familyName}  GPU text engine ready")
    }

    /** Runs one frame; returns `false` when the demo should stop. */
    fun frame(): Boolean {
        if (!running) return false

        while (true) {
            val event = SDL.pollEvent() ?: break
            when (event) {
                is SDLEvent.Quit -> running = false
                is SDLEvent.Window ->
                    if (event.type == SDLWindowEventType.CLOSE_REQUESTED) running = false
                is SDLEvent.Key ->
                    if (event.down && event.keycode == SDLKeycode.ESCAPE) running = false
                else -> Unit
            }
        }

        angle = (angle + 0.6) % 360.0

        val cmd = device.beginCommandBuffer() ?: return running
        val windowTexture = device.acquireSwapchainTexture(cmd, window)
        val target = windowTexture?.texture
        val vw = (windowTexture?.srcRect?.width ?: window.size.x).toFloat()
        val vh = (windowTexture?.srcRect?.height ?: window.size.y).toFloat()
        if (target != null) {
            val pass = cmd.beginRenderPass(
                colorTargets = listOf(
                    SDLGPUColorTargetInfo(
                        texture = target,
                        clearColor = SDLColor(18, 18, 24, 255),
                        loadOp = SDLGPULoadOp.CLEAR,
                        storeOp = SDLGPUStoreOp.STORE,
                    ),
                ),
            )
            if (pass != null) {
                pass.setViewport(SDLGPUViewport(0f, 0f, vw, vh))
                pass.setScissor(0, 0, vw.toInt(), vh.toInt())

                // Title: a rotating scaled piece of the text.
                drawText(pass, vw, vh, text, 40f, 40f, angle, (1f + 0.5f * kotlin.math.sin(angle / 40.0)).toFloat())
                drawText(pass, vw, vh, colored, 40f, 120f, 0.0, 1f)
                drawText(pass, vw, vh, sdfText, 40f, 220f, 0.0, 1f)

                pass.end()
                pass.close()
            }
            cmd.end()
        }
        device.submit(cmd)
        cmd.close()
        device.present(window)
        return running
    }

    /**
     * Uploads one text's draw sequences into the shared buffers and draws
     * them, offset/rotated/scaled around the top-left (px, py).
     */
    private fun drawText(
        pass: SDLGPURenderPass,
        vw: Float,
        vh: Float,
        text: SDLTTFText,
        px: Float,
        py: Float,
        angle: Double,
        scale: Float,
    ) {
        val sequences = text.getGPUDrawData() ?: return

        val interleaved = FloatArray(MAX_VERTICES * 4)
        val indices = IntArray(MAX_INDICES)
        val rad = angle * kotlin.math.PI / 180.0
        val cos = kotlin.math.cos(rad).toFloat()
        val sin = kotlin.math.sin(rad).toFloat()
        val rotScale = scale
        var vertexCount = 0
        var indexCount = 0

        // First pass: transform + interleave all vertices; record per-sequence
        // draw parameters.
        val draws = ArrayList<SequenceDraw>()
        var firstIndex = 0
        for (sequence in sequences) {
            val positions = sequence.positions
            val uvs = sequence.uvs
            val numVertices = positions.size / 2
            if (vertexCount + numVertices > MAX_VERTICES || indexCount + sequence.indices.size > MAX_INDICES) break

            val base = vertexCount
            for (i in 0 until numVertices) {
                val x = positions[i * 2]
                val y = positions[i * 2 + 1]
                // rotate + scale around the text origin
                val rx = (x * cos - y * sin) * scale
                val ry = (x * sin + y * cos) * scale
                // pixel -> NDC (positive Y upwards)
                val out = vertexCount * 4
                interleaved[out] = (px + rx) / vw * 2f - 1f
                interleaved[out + 1] = (py + ry) / vh * 2f - 1f
                interleaved[out + 2] = uvs[i * 2]
                interleaved[out + 3] = uvs[i * 2 + 1]
                vertexCount++
            }
            for (i in sequence.indices.indices) {
                indices[indexCount + i] = sequence.indices[i] + base
            }
            indexCount += sequence.indices.size
            draws.add(SequenceDraw(sequence.atlasTexture, sequence.imageType, firstIndex, sequence.indices.size))
            firstIndex += sequence.indices.size
        }
        if (vertexCount == 0) return

        vertexBuffer.setData(interleaved.toByteArray(0, vertexCount * VERTEX_STRIDE))
        val indexBytes = ByteArray(indexCount * 2)
        for (i in 0 until indexCount) {
            val v = indices[i]
            indexBytes[i * 2] = (v and 0xFF).toByte()
            indexBytes[i * 2 + 1] = ((v ushr 8) and 0xFF).toByte()
        }
        indexBuffer.setData(indexBytes)

        pass.bindVertexBuffers(vertexBuffer to 0)
        pass.bindIndexBuffer(indexBuffer, SDLGPUIndexElementSize.UINT16)

        var atlas: SDLGPUTexture? = null
        var lastImageType = -1
        for (draw in draws) {
            val atlasTexture = draw.atlasTexture ?: continue
            val imageType = draw.imageType
            if (atlas != atlasTexture || imageType != lastImageType) {
                atlas = atlasTexture
                lastImageType = imageType
                if (imageType == SDLTTFImageType.SDF) {
                    pass.bindGraphicsPipeline(sdfPipeline)
                } else {
                    pass.bindGraphicsPipeline(textPipeline)
                }
                pass.bindGraphicsTextureSamplers(0, atlas to sampler)
            }
            pass.drawIndexedPrimitives(
                indexCount = draw.indexCount,
                firstIndex = draw.firstIndex,
            )
        }
    }

    fun close() {
        text.close()
        colored.close()
        sdfText.close()
        engine.close()
        sdfFont.close()
        font.close()
        indexBuffer.close()
        vertexBuffer.close()
        sampler.close()
        sdfPipeline.close()
        textPipeline.close()
    }

    private class SequenceDraw(
        val atlasTexture: SDLGPUTexture?,
        val imageType: Int,
        val firstIndex: Int,
        val indexCount: Int,
    )
}

private fun FloatArray.toByteArray(offset: Int, length: Int): ByteArray {
    val out = ByteArray(length)
    for (i in 0 until length / 4) {
        val bits = this[offset / 4 + i].toRawBits()
        out[i * 4] = (bits ushr 0).toByte()
        out[i * 4 + 1] = (bits ushr 8).toByte()
        out[i * 4 + 2] = (bits ushr 16).toByte()
        out[i * 4 + 3] = (bits ushr 24).toByte()
    }
    return out
}

/**
 * Sets up the demo: SDL init, window, GPU device and fonts. Returns null
 * when no GPU device is available (e.g. headless with the dummy driver).
 */
fun createGpuTextDemo(fontPath: String): GpuTextDemo? {
    val window = try {
        SDL.createWindow(
            title = "sdl-ttf-kmp example-gpu",
            width = 800,
            height = 600,
            flags = SDLWindowFlags.RESIZABLE,
        )
    } catch (t: Throwable) {
        println("window creation failed: ${t.message}")
        return null
    }

    val device = SDLGPU.createDevice()
    if (device == null) {
        println("no GPU device available: ${SDL.error()}")
        window.close()
        return null
    }
    println("GPU drivers: ${SDLGPU.drivers}")
    println("GPU shader formats: 0x${device.shaderFormats.toString(16)}")

    if (!device.claimWindow(window)) {
        println("claiming the window failed: ${SDL.error()}")
        device.close()
        window.close()
        return null
    }

    return try {
        GpuTextDemo(window, device, fontPath)
    } catch (t: Throwable) {
        println("demo setup failed: ${t.message}")
        if (t is IllegalStateException) println("SDL_ttf error: ${SDLTTF.error()}")
        device.close()
        window.close()
        null
    }
}

fun runExample(fontPath: String?) {
    SDL.setMainReady()

    if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
        error("SDL_Init(VIDEO) failed: ${SDL.error()}")
    }
    println("SDL ${SDL.version()} (${SDL.revision()})")
    println("Video driver: ${SDL.getCurrentVideoDriver()}")

    if (!SDLTTF.init()) {
        error("TTF_Init failed: ${SDLTTF.error()}")
    }

    val path = resolveFont(fontPath)
    println("font: $path")

    val demo = createGpuTextDemo(path) ?: run {
        SDLTTF.quit()
        SDL.quit()
        return
    }

    var frames = 0
    val start = SDL.getTicks()
    while (demo.frame()) {
        frames++
        if (frames % 120 == 0) {
            val elapsedMs = (SDL.getTicks() - start).toFloat() / 1000f
            println("fps: ${(frames / elapsedMs).toInt()}")
        }
        SDL.delay(16)
    }
    println("ran $frames frames")

    demo.close()
    SDLTTF.quit()
    SDL.quit()
}

/** Finds a usable font file by trying [arg], then common system fonts. */
fun resolveFont(arg: String?): String {
    val candidates = buildList {
        arg?.takeIf { it.isNotBlank() }?.let(::add)
        add("/System/Library/Fonts/Hiragino Sans GB.ttc")
        add("/System/Library/Fonts/PingFang.ttc")
        add("/System/Library/Fonts/Helvetica.ttc")
        add("fonts/DejaVuSans.ttf")
        add("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")
        add("C:/Windows/Fonts/arial.ttf")
    }.distinct()

    for (path in candidates) {
        try {
            val probe = SDLTTF.openFont(path, 12f)
            probe.close()
            return path
        } catch (_: Throwable) {
            // keep looking
        }
    }
    error("no usable font found, tried: $candidates")
}
