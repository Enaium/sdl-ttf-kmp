# wasm build assets

The `wasmJs` target cannot use cinterop, so SDL3 and SDL_ttf are compiled to
one Emscripten module here and driven through a JS glue layer.

| File | Origin |
|------|--------|
| `CMakeLists.txt` | this repository: builds SDL3 + SDL_ttf (vendored FreeType/HarfBuzz/plutosvg) as static libraries for Emscripten |
| `ttf_wasm_shim.c` | this repository: handle-based wrapper over the SDL_ttf API used by the Kotlin bindings |
| `ttf_kmp_glue.js` | this repository: initializes the module through sdl-kmp's glue and installs the `sdl_ttf_kmp_*` globals |
| `sdl_wasm_shim.c` | vendored from [sdl-kmp](https://github.com/Enaium/sdl-kmp) (`sdl-kmp/wasm/sdl_wasm_shim.c`) |
| `sdl_kmp_glue.js` | vendored from sdl-kmp (`sdl-kmp/wasm/sdl_kmp_glue.js`); identical to the one in `@enaium/sdl-kmp` |
| `emscripten_overrides.js` | vendored from sdl-kmp (`sdl-kmp/wasm/emscripten_overrides.js`) |

The vendored files must match the `sdl-kmp` version in
`gradle/libs.versions.toml`: the Kotlin `wasmJs` klib calls the globals those
files install. When bumping sdl-kmp, re-copy them from the same revision.

SDL3 must be part of the module: TTF-rendered `SDL_Surface` handles are used by
sdl-kmp's renderer/surface bindings, which operate on the module heap. The
module therefore replaces `@enaium/sdl-kmp`'s SDL-only module for web apps;
initialize it with `initSdlTtfKmp()` from the `@enaium/sdl-ttf-kmp` npm
package (see the main README). The package also exports `initSdlKmp` as an
alias of the same function, so both names can be called idempotently.

This differs from sdl-image-kmp, whose module can coexist with sdl-kmp's:
its API only copies pixel bytes between the two wasm instances. SDL_ttf's
renderer text engine calls into the application's `SDL_Renderer`, which is a
pointer into sdl-kmp's heap and cannot be used from another instance, so a
single combined module is required.

Build and stage the npm package:

```bash
./gradlew :sdl-ttf-kmp:assembleWasmNpm   # -> sdl-ttf-kmp/build/npm
```

`linkWasmTtf` locates the Emscripten SDK via `$EMSDK`,
`-Pwasm.emsdk=<dir>`, `~/emsdk` or `~/sdk/emsdk`.
