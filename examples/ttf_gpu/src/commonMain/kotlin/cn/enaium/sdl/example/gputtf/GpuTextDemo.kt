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
import cn.enaium.sdl.SDLGPUBlendFactor
import cn.enaium.sdl.SDLGPUBlendOp
import cn.enaium.sdl.SDLGPUBlendState
import cn.enaium.sdl.SDLGPUColorComponent
import cn.enaium.sdl.SDLGPUColorTargetDescription
import cn.enaium.sdl.SDLGPUColorTargetInfo
import cn.enaium.sdl.SDLGPUDevice
import cn.enaium.sdl.SDLGPUFilter
import cn.enaium.sdl.SDLGPUGraphicsPipeline
import cn.enaium.sdl.SDLGPUGraphicsPipelineCreateInfo
import cn.enaium.sdl.SDLGPUIndexElementSize
import cn.enaium.sdl.SDLGPULoadOp
import cn.enaium.sdl.SDLGPUPrimitiveType
import cn.enaium.sdl.SDLGPURasterizerState
import cn.enaium.sdl.SDLGPURenderPass
import cn.enaium.sdl.SDLGPUSampler
import cn.enaium.sdl.SDLGPUSamplerCreateInfo
import cn.enaium.sdl.SDLGPUShader
import cn.enaium.sdl.SDLGPUShaderFormat
import cn.enaium.sdl.SDLGPUShaderStage
import cn.enaium.sdl.SDLGPUStoreOp
import cn.enaium.sdl.SDLGPUTexture
import cn.enaium.sdl.SDLGPUTextureCreateInfo
import cn.enaium.sdl.SDLGPUTextureFormat
import cn.enaium.sdl.SDLGPUTextureUsage
import cn.enaium.sdl.SDLGPUVertexAttribute
import cn.enaium.sdl.SDLGPUVertexBufferDescription
import cn.enaium.sdl.SDLGPUVertexElementFormat
import cn.enaium.sdl.SDLGPUVertexInputRate
import cn.enaium.sdl.SDLGPUVertexInputState
import cn.enaium.sdl.SDLGPUViewport
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLKeycode
import cn.enaium.sdl.SDLPixelFormat
import cn.enaium.sdl.SDLWindow
import cn.enaium.sdl.SDLWindowEventType
import cn.enaium.sdl.SDLWindowFlags
import cn.enaium.sdl.SDLEvent
import cn.enaium.sdl.SDLSurface
import cn.enaium.sdl.ttf.SDLTTF
import cn.enaium.sdl.ttf.SDLTTFFont
import cn.enaium.sdl.ttf.SDLTTFImageType
import cn.enaium.sdl.ttf.SDLTTFStyle
import cn.enaium.sdl.ttf.SDLTTFText
import cn.enaium.sdl.ttf.SDLTTFTextEngine

/** Vertex stride: float2 position + float2 uv. */
private const val VERTEX_STRIDE = 16
private const val MAX_VERTICES = 16384
private const val MAX_INDICES = 32768

/**
 * The SDL_ttf GPU demo: lays text out with the SDL_ttf GPU text engine and
 * renders it with the SDL3 GPU API (sdl-kmp's SDLGPU bindings), entirely
 * from commonMain.
 *
 * It covers the same use cases as the 2D renderer example:
 *
 *  - text laid out by the GPU engine ([SDLTTF.createGPUTextEngine] +
 *    [SDLTTFText.getGPUDrawData]) and drawn with indexed primitives;
 *  - rotated/scaled text (per-vertex NDC transform on the CPU);
 *  - SDF text rendered through a dedicated SDF pipeline (smoothstep on the
 *    signed distance stored in the atlas alpha);
 *  - the surface renderers ([SDLTTF.renderTextBlended] and friends)
 *    uploaded into GPU textures and drawn as quads;
 *  - font styles (bold/italic), CJK text, colors, interactive resizing.
 *
 * Coordinate notes (SDL_GPU conventions): NDC has +Y upwards, the viewport
 * origin is top-left with +Y down, and texture coordinates are top-left
 * origin with +Y down. TTF_GetGPUTextDrawData vertices are pixels with the
 * origin at the text's bottom-left and +Y upwards.
 */
class GpuTextDemo(
    private val window: SDLWindow,
    private val device: SDLGPUDevice,
    private val fontPath: String,
) {
    private val font: SDLTTFFont = SDLTTF.openFont(fontPath, 48f)
    private val styleFont: SDLTTFFont = SDLTTF.openFont(fontPath, 28f)
    private val engine: SDLTTFTextEngine = SDLTTF.createGPUTextEngine(device)

    private val title: SDLTTFText = SDLTTF.createText(engine, font, "SDL_ttf GPU text engine")
    private val colored: SDLTTFText = SDLTTF.createText(engine, font, "GPU API - rendered from commonMain")
    private val cjk: SDLTTFText = SDLTTF.createText(engine, font, "中文渲染")
    private val styled: SDLTTFText = SDLTTF.createText(engine, styleFont, "Bold + Italic styles")
    private val sdfFont: SDLTTFFont = SDLTTF.openFont(fontPath, 64f).also { it.SDF = true }
    private val sdfText: SDLTTFText = SDLTTF.createText(engine, sdfFont, "SDF: sharp at any scale")

    private val textPipeline: SDLGPUGraphicsPipeline
    private val sdfPipeline: SDLGPUGraphicsPipeline
    private val sampler: SDLGPUSampler
    private val vertexBuffer: SDLGPUBuffer
    private val indexBuffer: SDLGPUBuffer

    /** Surface-rendered samples uploaded into GPU textures (quad demo). */
    private val surfaceTextures: List<Pair<SDLGPUTexture, Pair<Int, Int>>>

    private var angle = 0.0
    private var colorIndex = 0
    private var fontGrow = true
    private var running = true

    private val palette = listOf(
        SDLColor(255, 200, 60),
        SDLColor(80, 255, 160),
        SDLColor(120, 180, 255),
        SDLColor(255, 120, 200),
    )

    init {
        colored.color = SDLColor(180, 200, 255)
        cjk.color = SDLColor(255, 200, 100)
        styled.color = SDLColor(160, 220, 160)
        sdfText.color = SDLColor(255, 255, 255)
        styleFont.style = SDLTTFStyle.BOLD or SDLTTFStyle.ITALIC

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

        // Glyph coverage lives in the atlas alpha channel: alpha blending is
        // required (the default opaque blend would draw solid quads).
        val blendState = SDLGPUBlendState(
            srcColorBlendFactor = SDLGPUBlendFactor.SRC_ALPHA,
            dstColorBlendFactor = SDLGPUBlendFactor.ONE_MINUS_SRC_ALPHA,
            colorBlendOp = SDLGPUBlendOp.ADD,
            srcAlphaBlendFactor = SDLGPUBlendFactor.SRC_ALPHA,
            dstAlphaBlendFactor = SDLGPUBlendFactor.ONE_MINUS_SRC_ALPHA,
            alphaBlendOp = SDLGPUBlendOp.ADD,
            colorWriteMask = SDLGPUColorComponent.R or SDLGPUColorComponent.G or
                SDLGPUColorComponent.B or SDLGPUColorComponent.A,
        )
        val colorTarget = SDLGPUColorTargetDescription(format = windowFormat, blendState = blendState)

        textPipeline = device.createGraphicsPipeline(
            SDLGPUGraphicsPipelineCreateInfo(
                vertexShader = vertexShader,
                fragmentShader = textFragment,
                vertexInputState = vertexInputState,
                primitiveType = SDLGPUPrimitiveType.TRIANGLELIST,
                rasterizerState = SDLGPURasterizerState(),
                targetDescriptions = listOf(colorTarget),
            ),
        ) ?: error("text pipeline creation failed: ${SDL.error()}")

        sdfPipeline = device.createGraphicsPipeline(
            SDLGPUGraphicsPipelineCreateInfo(
                vertexShader = vertexShader,
                fragmentShader = sdfFragment,
                vertexInputState = vertexInputState,
                primitiveType = SDLGPUPrimitiveType.TRIANGLELIST,
                rasterizerState = SDLGPURasterizerState(),
                targetDescriptions = listOf(colorTarget),
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

        // Surface renderers -> GPU textures (the renderer example's
        // "surface to texture" use case).
        surfaceTextures = listOf(
            Triple("Blended: alpha antialiasing", SDLColor(240, 240, 240), 0),
            Triple("Shaded: solid background", SDLColor(120, 220, 160), 1),
            Triple("LCD subpixel rendering", SDLColor(140, 210, 255), 2),
        ).map { (label, color, mode) ->
            val f = SDLTTF.openFont(fontPath, 24f)
            val surface = when (mode) {
                1 -> SDLTTF.renderTextShaded(f, label, color, SDLColor(20, 40, 30))
                2 -> SDLTTF.renderTextLCD(f, label, color, SDLColor(10, 15, 25))
                else -> SDLTTF.renderTextBlended(f, label, color)
            }
            f.close()
            val w = surface?.width ?: 0
            val h = surface?.height ?: 0
            val tex = surfaceToTexture(surface, label)
            tex to Pair(w, h)
        }

        println("font: ${font.familyName}  GPU text engine ready")
    }

    private fun surfaceToTexture(surface: SDLSurface?, label: String): SDLGPUTexture {
        checkNotNull(surface) { "surface render of '$label' failed: ${SDLTTF.error()}" }
        val tex = device.createTexture(
            SDLGPUTextureCreateInfo(
                format = SDLGPUTextureFormat.B8G8R8A8_UNORM,
                usage = SDLGPUTextureUsage.SAMPLE,
                width = surface.width,
                height = surface.height,
            ),
        ) ?: error("texture creation failed: ${SDL.error()}")
        check(tex.upload(surface.pixels, surface.pitch, 0, 0, surface.width, surface.height)) {
            "texture upload failed: ${SDL.error()}"
        }
        surface.close()
        return tex
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
                is SDLEvent.Key -> when {
                    !event.down -> Unit
                    event.keycode == SDLKeycode.ESCAPE -> running = false
                    event.keycode == SDLKeycode.SPACE -> {
                        fontGrow = !fontGrow
                        println("SDF scale animation ${if (fontGrow) "on" else "off"}")
                    }
                    event.keycode == SDLKeycode.C -> {
                        colorIndex = (colorIndex + 1) % palette.size
                        println("color -> ${palette[colorIndex]}")
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
                else -> Unit
            }
        }

        angle = (angle + 0.6) % 360.0
        colored.color = palette[colorIndex]

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

                // Title: rotating + scaling text.
                val titleScale = 1f + 0.5f * kotlin.math.sin(angle / 40.0).toFloat()
                drawText(pass, vw, vh, title, 40f, 40f, angle, titleScale)
                drawText(pass, vw, vh, colored, 40f, 130f, 0.0, 1f)
                drawText(pass, vw, vh, cjk, 40f, 220f, 0.0, 1f)
                drawText(pass, vw, vh, styled, 40f, 300f, 0.0, 1f)

                // SDF text: scaling animates when SPACE enables it.
                val sdfScale = if (fontGrow) 1f + 0.25f * kotlin.math.sin(angle / 30.0).toFloat() else 1f
                drawText(pass, vw, vh, sdfText, 40f, 360f, 0.0, sdfScale)

                // Surface-rendered samples drawn as quads.
                var y = 470f
                for ((texture, size) in surfaceTextures) {
                    drawSurfaceQuad(pass, vw, vh, texture, 40f, y, size.first.toFloat(), size.second.toFloat())
                    y += size.second + 10f
                }

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
        val rad = angle * kotlin.math.PI / 180.0
        val cos = kotlin.math.cos(rad).toFloat()
        val sin = kotlin.math.sin(rad).toFloat()
        // TTF GPU vertices are pixels with the origin at the text's bottom-left
        // and positive Y upwards; (px, py) is the top-left screen position
        // (py grows downwards), so screenY = py + textHeight - y.
        val textHeight = text.size?.y?.toFloat() ?: 0f
        val interleaved = FloatArray(MAX_VERTICES * 4)
        val indices = IntArray(MAX_INDICES)
        var vertexCount = 0
        var indexCount = 0

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
                // pixel -> NDC (SDL_GPU NDC: +1 at the top of the viewport;
                // screen y grows downwards, TTF y grows upwards)
                val out = vertexCount * 4
                val screenY = py + textHeight - ry
                interleaved[out] = (px + rx) / vw * 2f - 1f
                interleaved[out + 1] = 1f - screenY / vh * 2f
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

    /** Draws a texture-backed surface as a full quad at screen position (px, py). */
    private fun drawSurfaceQuad(
        pass: SDLGPURenderPass,
        vw: Float,
        vh: Float,
        texture: SDLGPUTexture,
        px: Float,
        py: Float,
        width: Float,
        height: Float,
    ) {
        val x0 = px / vw * 2f - 1f
        val y0 = 1f - py / vh * 2f
        val x1 = (px + width) / vw * 2f - 1f
        val y1 = 1f - (py + height) / vh * 2f
        val quad = floatArrayOf(
            // position (x, y)   uv (u, v)
            x0, y0, 0f, 0f,
            x1, y0, 1f, 0f,
            x0, y1, 0f, 1f,
            x0, y1, 0f, 1f,
            x1, y0, 1f, 0f,
            x1, y1, 1f, 1f,
        )
        vertexBuffer.setData(quad.toByteArray(0, quad.size * 4))
        pass.bindGraphicsPipeline(textPipeline)
        pass.bindGraphicsTextureSamplers(0, texture to sampler)
        pass.bindVertexBuffers(vertexBuffer to 0)
        pass.drawPrimitives(vertexCount = 6)
    }

    fun close() {
        surfaceTextures.forEach { it.first.close() }
        title.close()
        colored.close()
        cjk.close()
        styled.close()
        sdfText.close()
        engine.close()
        styleFont.close()
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
