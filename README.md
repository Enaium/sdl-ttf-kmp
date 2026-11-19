# sdl-ttf-kmp

Kotlin Multiplatform bindings for [SDL_ttf 3](https://github.com/libsdl-org/SDL_ttf) (TrueType font rendering with FreeType/HarfBuzz), built on top of [sdl-kmp](https://github.com/Enaium/sdl-kmp). The public API lives in the `cn.enaium.sdl.ttf` package and works directly with the sdl-kmp types (`SDLRenderer`, `SDLSurface`, `SDLIOStream`, `SDLColor`, ...).

Three implementations, mirroring sdl-kmp:

- **JVM**: SDL3 and SDL_ttf (with the vendored FreeType/HarfBuzz/plutosvg from this repository's `SDL_ttf` submodule) are compiled by CMake (`jni/`) into a JNI shared library (`libsdl_ttf_jni`), shipped as per-OS/arch `sdl-ttf-kmp-jni-jvm-*` artifacts — the same self-contained approach as sdl-kmp's `libsdl_jni`. `TtfNativeLoader` extracts the matching binary at runtime. The process contains a second SDL3 copy; SDL_ttf errors are read through the TTF-side `SDL_GetError` (`SDLTTF.error()`), and SDL objects from the sdl-kmp library are operated on through SDL3's function-pointer interfaces, so the copies do not interfere.
- **Native (Kotlin/Native)**: the SDL_ttf static library (including FreeType/HarfBuzz/plutosvg) is compiled per target with CMake and **embedded into the published klib**. SDL3 itself is not compiled: the SDL3 symbols are resolved at the consumer's final link from the sdl-kmp klib, which is always present because the bindings use the `cn.enaium.sdl` types.
- **Web (Kotlin/Wasm)**: SDL3 and SDL_ttf are compiled to a single Emscripten module and driven through a JS glue layer. Kotlin/Wasm has no cinterop, so the `wasmJs` actuals call a flat, handle-based C shim. Because TTF-rendered `SDL_Surface`s must interoperate with sdl-kmp's renderer/surface bindings, the module also embeds sdl-kmp's SDL3 shim and exposes the `sdl_kmp_*` functions on the same Emscripten heap. The module and glue ship as the [`@enaium/sdl-ttf-kmp`](https://www.npmjs.com/package/@enaium/sdl-ttf-kmp) npm package.

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
| Web      | `wasmJs`                                            | Emscripten module: SDL3 + SDL_ttf in one heap, npm package (`@enaium/sdl-ttf-kmp`) |

## Usage

The published version requires [sdl-kmp](https://github.com/Enaium/sdl-kmp) `1.0.14-fixed.3.4.18` (it is an `api` dependency, pulled in automatically).

`build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.enaium.sdl:sdl-ttf-kmp:1.0.1-fixed.3.2.2")
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
            check(textSurface.lock()) { "failed to lock surface" }
            texture.update(null, textSurface.pixels, textSurface.pitch)
            textSurface.unlock()
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

### Web (wasmJs)

The `wasmJs` target needs the `@enaium/sdl-ttf-kmp` npm package, which ships the combined SDL3 + SDL_ttf Emscripten module and its JS glue. It **replaces** `@enaium/sdl-kmp`'s SDL-only module for web apps: `initSdlTtfKmp` installs both the `sdl_kmp_*` globals used by sdl-kmp's `wasmJs` actuals and the `sdl_ttf_kmp_*` globals used by this library, on one Emscripten heap, so TTF surfaces interoperate with sdl-kmp's renderer/surface APIs.

Unlike [sdl-image-kmp](https://github.com/Enaium/sdl-image-kmp), which can initialize its own module next to sdl-kmp's because its API only copies pixel bytes between the two instances, SDL_ttf's renderer text engine must draw on the application's `SDL_Renderer`. A renderer pointer cannot cross Emscripten instances, so both libraries have to share one heap — hence one combined module instead of two independent inits.

The package exports `initSdlKmp` as an alias of `initSdlTtfKmp`; both initialize the same combined module, so the two-init pattern from sdl-image-kmp's examples is safe (the second call is a no-op, not a second instance). Mixing in `@enaium/sdl-kmp`'s own `initSdlKmp` is order-sensitive: calling it before this package's init is superseded by the combined module (works, but loads an unused module); calling it after replaces the `sdl_kmp_*` globals with a different module and invalidates TTF handles.

```bash
npm install @enaium/sdl-ttf-kmp
```

```html
<script type="importmap">
  { "imports": { "@enaium/sdl-ttf-kmp": "./node_modules/@enaium/sdl-ttf-kmp/ttf_kmp_glue.js" } }
</script>
<script type="module">
  import { initSdlKmp, initSdlTtfKmp } from '@enaium/sdl-ttf-kmp';

  const canvas = document.getElementById('canvas');
  // initSdlKmp is an alias of initSdlTtfKmp (same combined module).
  await initSdlKmp({ canvas });
  await initSdlTtfKmp();          // idempotent

  // Optional: expose a font file to TTF_OpenFont through the module's
  // virtual filesystem (TTF_OpenFont takes a path, not a URL).
  const bytes = new Uint8Array(await (await fetch('./fonts/DejaVuSans.ttf')).arrayBuffer());
  sdlTtfKmpWriteFile('/fonts/DejaVuSans.ttf', bytes);

  await import('./app.mjs');   // the Kotlin/Wasm executable
</script>
```

`initSdlTtfKmp()` returns the Emscripten `Module` and installs `sdlTtfKmpWriteFile(path, bytes)`. The npm version is the Gradle version (`1.0.1-fixed.3.2.2`). The bundled SDL3 build has no WebGPU backend, so `SDLTTF.createGPUTextEngine` is unavailable on wasm.

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
- **Web (wasmJs)**: the combined module is built by `:sdl-ttf-kmp:linkWasmTtf` (requires the [Emscripten SDK](https://emscripten.org/); it is located via `$EMSDK`, `-Pwasm.emsdk=...`, `~/emsdk` or `~/sdk/emsdk`). Initialize with `initSdlTtfKmp` from `@enaium/sdl-ttf-kmp` (its `initSdlKmp` export is an idempotent alias); `@enaium/sdl-kmp`'s own init must not run after it. `TTF_OpenFont` reads the module's virtual filesystem; write fonts with `sdlTtfKmpWriteFile` or open them from an `SDLIOStream` (`SDLTTF.openFontIO`). GPU text engines are unavailable because the bundled SDL3 has no WebGPU backend. `SDLSurface.pixels` requires `lock()`/`unlock()` on wasm (this holds for all surfaces, including ones created by sdl-kmp).
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

- **`examples/ttf_renderer/browser`** — the wasmJs runner for the demo above, consuming the staged `@enaium/sdl-ttf-kmp` npm package. `copyWebAssets` produces a self-contained static app in `build/browserApp/` (page, fonts, npm package, Kotlin/Wasm output) that also runs under Node:

```bash
./gradlew :examples:ttf_renderer:browser:copyWebAssets
# headless (Node, dummy drivers)
node examples/ttf_renderer/browser/build/browserApp/browser-node-test.mjs
# with a browser
python3 -m http.server -d examples/ttf_renderer/browser/build/browserApp
```

- **`examples/ttf_gpu`** — text laid out with the SDL_ttf GPU text engine
  (`SDLTTF.createGPUTextEngine` + `SDLTTFText.getGPUDrawData`) and rendered
  with the SDL3 GPU API from commonMain: MSL shaders on Metal, precompiled
  SPIR-V elsewhere; per frame the pixel-space vertices are transformed to
  NDC on the CPU, uploaded into vertex/index buffers and drawn with
  `drawIndexedPrimitives`. A dedicated SDF pipeline (smoothstep on the
  signed distance) renders SDF fonts sharp at any scale. Requires a GPU
  (not available with the dummy driver).

## Building from source

Requirements: JDK 21, CMake, a C/C++ compiler; Xcode for Apple targets, the `x86_64-w64-mingw32-gcc` toolchain for MinGW cross-compiles (Linux host), the Android NDK for `androidNative*`, the Emscripten SDK for `wasmJs`.

```bash
git clone --recurse-submodules git@github.com:Enaium/sdl-ttf-kmp.git
cd sdl-ttf-kmp

# compile + test the JVM target
./gradlew :sdl-ttf-kmp:jvmTest

# run the example headless
SDL_VIDEO_DRIVER=dummy ./gradlew :examples:ttf_renderer:jvmRun

# build the wasm module + npm package, then the browser example
./gradlew :sdl-ttf-kmp:assembleWasmNpm :examples:ttf_renderer:browser:copyWebAssets

# publish everything buildable on this host to Maven Local
./gradlew :sdl-ttf-kmp:publishToMavenLocal :ttf-jni-jvm-darwin-aarch64:publishToMavenLocal
```

## CI

Both workflows are **manually triggered** (Actions tab):

- **`test.yml`** — local Maven publish + test: publishes every artifact the runner can build to Maven Local (no signing, no secrets), runs the JVM/native tests, the headless example, and the wasm pipeline (Emscripten module, npm package, browser example under Node). Use this to verify a change before publishing.
- **`publish.yml`** — formal release: publishes the metadata + JVM module, all target klibs (including `wasmJs`) and the JNI artifacts to Maven Central, signed with PGP, then publishes the `@enaium/sdl-ttf-kmp` npm package. The version is the one in `build.gradle.kts`. Requires the repository secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`, `SIGNING_KEY_ID`, `SIGNING_PASSWORD` and `NPM_PUBLISH_KEY` (npm automation token).

## License

MIT. The bundled SDL3 submodule is licensed under the [zlib license](https://github.com/libsdl-org/SDL/blob/main/LICENSE.txt).
