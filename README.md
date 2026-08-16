# sdl-ttf-kmp

Kotlin Multiplatform bindings for [SDL_ttf 3](https://github.com/libsdl-org/SDL_ttf) (TrueType font rendering with FreeType/HarfBuzz), built on top of [sdl-kmp](https://github.com/Enaium/sdl-kmp). The public API lives in the `cn.enaium.sdl.ttf` package and works directly with the sdl-kmp types (`SDLRenderer`, `SDLSurface`, `SDLIOStream`, `SDLColor`, ...).

Two implementations, mirroring sdl-kmp:

- **JVM**: SDL3 and SDL_ttf (with the vendored FreeType/HarfBuzz/plutosvg from this repository's `SDL_ttf` submodule) are compiled by CMake (`jni/`) into a JNI shared library (`libsdl_ttf_jni`), shipped as per-OS/arch `sdl-ttf-kmp-jni-jvm-*` artifacts — the same self-contained approach as sdl-kmp's `libsdl_jni`. `TtfNativeLoader` extracts the matching binary at runtime. The process contains a second SDL3 copy; SDL_ttf errors are read through the TTF-side `SDL_GetError` (`SDLTTF.error()`), and SDL objects from the sdl-kmp library are operated on through SDL3's function-pointer interfaces, so the copies do not interfere.
- **Native (Kotlin/Native)**: the SDL_ttf static library (including FreeType/HarfBuzz/plutosvg) is compiled per target with CMake and **embedded into the published klib**. SDL3 itself is not compiled: the SDL3 symbols are resolved at the consumer's final link from the sdl-kmp klib, which is always present because the bindings use the `cn.enaium.sdl` types.

## Supported platforms

| Platform | Targets                                             | Implementation                                  |
|----------|-----------------------------------------------------|-------------------------------------------------|
| JVM      | `jvm` (Linux/macOS/Windows)                         | JNI shared library (`libsdl_ttf_jni`), SDL3 + SDL_ttf compiled from source |
| macOS    | `macosArm64`, `macosX64`                            | cinterop + embedded static SDL_ttf              |
| Linux    | `linuxX64`, `linuxArm64`                            | cinterop + embedded static SDL_ttf              |
| Windows  | `mingwX64`                                          | cinterop + embedded static SDL_ttf              |
| iOS      | `iosArm64`, `iosX64`, `iosSimulatorArm64`           | cinterop + embedded static SDL_ttf              |
| tvOS     | `tvosArm64`, `tvosSimulatorArm64`                   | cinterop + embedded static SDL_ttf              |
| Android  | `androidNativeArm64`, `androidNativeArm32`, `androidNativeX64`, `androidNativeX86` | cinterop + embedded static SDL_ttf (built with the NDK) |

## Usage

The published version requires [sdl-kmp](https://github.com/Enaium/sdl-kmp) `1.0.7` (it is an `api` dependency, pulled in automatically).

`build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.enaium.sdl:sdl-ttf-kmp:1.0.0")
        }
    }
}
```

```kotlin
import cn.enaium.sdl.*
import cn.enaium.sdl.ttf.*

fun main() {
    SDL.setMainReady()

    if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
        error("SDL_Init failed: ${SDL.error()}")
    }
    if (!SDLTTF.init()) {
        error("TTF_Init failed: ${SDLTTF.error()}")
    }

    SDL.createWindow("sdl-ttf-kmp", 800, 600).use { window ->
        SDL.createRenderer(window).use { renderer ->
            val font = SDLTTF.openFont("/path/to/font.ttf", 32f)

            // 1) Render text to a surface, upload it into a texture and draw it.
            val textSurface = SDLTTF.renderTextBlended(font, "Hello, SDL_ttf!", SDLColor(240, 240, 240))
                ?: error("render failed: ${SDLTTF.error()}")
            val texture = renderer.createTexture(
                format = textSurface.format,
                access = SDLTextureAccess.STATIC,
                width = textSurface.width,
                height = textSurface.height,
            )
            texture.update(null, textSurface.pixels, textSurface.pitch)
            textSurface.close()

            // 2) Or use the renderer text engine: SDL_ttf draws directly on
            //    the sdl-kmp renderer, and the text re-lays out automatically.
            val engine = SDLTTF.createRendererTextEngine(renderer)
            val text = SDLTTF.createText(engine, font, "Live text engine")
            text.color = SDLColor(255, 200, 60)
            text.wrapWidth = 300

            renderer.drawColor = SDLColor(18, 18, 24)
            renderer.clear()
            renderer.renderTexture(texture, dst = SDLFRect(40f, 40f, texture.size.x, texture.size.y))
            text.draw(40f, 120f)
            renderer.present()

            text.close()
            engine.close()
            font.close()
        }
    }

    SDLTTF.quit()
    SDL.quit()
}
```

### API overview

- **Fonts**: `SDLTTF.openFont`, `SDLTTF.openFontIO` (from a `cn.enaium.sdl.SDLIOStream`), `SDLTTF.copyFont`; `SDLTTFFont` exposes size/style/outline/hinting/SDF/kerning/direction/char-spacing, metrics (height/ascent/descent/lineSkip/weight), `getStringSize`, `getStringSizeWrapped`, `measureString`, glyph metrics/kerning and fallback fonts.
- **Rendering to surfaces**: `renderTextSolid/Shaded/Blended/LCD` (+ `_Wrapped`) and `renderGlyph*` return `SDLSurface`s; wrap them into a texture with `renderer.createTexture` + `SDLTexture.update` (see the note below).
- **Text engines**: `SDLTTF.createRendererTextEngine(renderer)` draws `SDLTTFText` objects directly on an sdl-kmp `SDLRenderer` via `SDLTTFText.draw`; `SDLTTF.createSurfaceTextEngine` draws on software surfaces via `SDLTTFText.drawOnSurface`. Text objects support color, position, wrap width, wrap-whitespace visibility, string editing and substring lookup (`subStringAt`/`subStringForLine`/`subStringForPoint`).
- **Errors**: every function either returns null/false or throws; the last error is available via `SDLTTF.error()`.

### Platform notes

- **Kotlin version compatibility**: the published klibs are built with Kotlin 2.4.x. Keep the consumer's Kotlin version in sync (the same rule applies to sdl-kmp).
- **macOS JVM**: requires `-XstartOnFirstThread` (the example `jvmRun` task already sets it). With a HiDPI window, rasterize text at `sizeInPixels / size` and draw at logical size for crisp output (the example does this; see `TTFTextDemo.dpiScale`).
- **JVM native library**: the matching `sdl-ttf-kmp-jni-jvm-{os}-{arch}` artifact is a transitive runtime dependency; `TtfNativeLoader` extracts `libsdl_ttf_jni` and `System.load()`s it. `libsdl_ttf_jni` bundles its own SDL3, so no `java.library.path` setup is needed.
- **Text textures**: `SDLTexture.createTextureFromSurface` downcasts the surface to sdl-kmp's internal implementation, so SDL_ttf-rendered surfaces must be uploaded with `createTexture` + `SDLTexture.update` instead.
- **Android**: building an `androidNative*` target requires an installed Android NDK (found under `$ANDROID_HOME/ndk`); the SDL_ttf static library is cross-compiled with its CMake toolchain.
- **Headless / CI**: set `SDL_VIDEO_DRIVER=dummy` (hint or environment variable) to run without a display; SDL_ttf itself does not need video.

## Examples

- **`examples/ttf_renderer`** — a renderer demo on top of sdl-kmp's 2D renderer: surface-rendered text (blended/shaded/LCD/supersampled) uploaded into textures, the renderer text engine (animated, wrapped, resizable text), the surface text engine (drawing on a software surface), fonts opened from files and `SDL_IOStream`s, metrics/kerning/substring inspection, and CJK font resolution. Runs on JVM, macOS, Linux and Windows (MinGW):

```bash
# headless (CI / servers)
SDL_VIDEO_DRIVER=dummy ./gradlew :examples:ttf_renderer:jvmRun --args="fonts/DejaVuSans.ttf"
SDL_VIDEO_DRIVER=dummy ./gradlew :examples:ttf_renderer:runDebugExecutableMacosArm64

# with a window
./gradlew :examples:ttf_renderer:jvmRun
```

Controls: `ESC` quit, `UP`/`DOWN` font size, `SPACE` toggle wrapping, `W` wrap whitespace, `C` cycle color, click to inspect substrings.

- **`examples/ttf_gpu`** — text laid out with the SDL_ttf GPU text engine
  (`SDLTTF.createGPUTextEngine` + `SDLTTFText.getGPUDrawData`) and rendered
  with the SDL3 GPU API from commonMain: MSL shaders on Metal, precompiled
  SPIR-V elsewhere; per frame the pixel-space vertices are transformed to
  NDC on the CPU, uploaded into vertex/index buffers and drawn with
  `drawIndexedPrimitives`. A dedicated SDF pipeline (smoothstep on the
  signed distance) renders SDF fonts sharp at any scale. Requires a GPU
  (not available with the dummy driver).

## Building from source

Requirements: JDK 21, CMake, a C/C++ compiler; Xcode for Apple targets, the `x86_64-w64-mingw32-gcc` toolchain for MinGW cross-compiles (Linux host), the Android NDK for `androidNative*`.

```bash
git clone --recurse-submodules git@github.com:Enaium/sdl-ttf-kmp.git
cd sdl-ttf-kmp

# compile + test the JVM target
./gradlew :sdl-ttf-kmp:jvmTest

# run the example headless
SDL_VIDEO_DRIVER=dummy ./gradlew :examples:ttf_renderer:jvmRun

# publish everything buildable on this host to Maven Local
./gradlew :sdl-ttf-kmp:publishToMavenLocal :ttf-jni-jvm-darwin-aarch64:publishToMavenLocal
```

## CI

Both workflows are **manually triggered** (Actions tab):

- **`test.yml`** — local Maven publish + test: publishes every artifact the runner can build to Maven Local (no signing, no secrets), runs the JVM/native tests and the example headless. Use this to verify a change before publishing.
- **`publish.yml`** — formal Maven Central release: publishes the metadata + JVM module, all target klibs and the JNI artifacts to Maven Central, signed with PGP. The version is taken from the workflow input (`-PsdlTtfVersion`, default `1.0.0`). Requires the repository secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`, `SIGNING_KEY_ID` and `SIGNING_PASSWORD`.

## License

## License

MIT. The bundled SDL3 submodule is licensed under the [zlib license](https://github.com/libsdl-org/SDL/blob/main/LICENSE.txt).
