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

/*
 * Glue between the combined Emscripten module (sdl_wasm.js: SDL3 + SDL_ttf +
 * both shims) and Kotlin/Wasm.
 *
 * The module is the same one initialized by sdl-kmp's glue: calling
 * initSdlKmp() first installs every sdl_kmp_* global the sdl-kmp wasmJs
 * bindings use, then this layer installs the sdl_ttf_kmp_* globals on top of
 * the very same instance. There is exactly one Emscripten heap, so
 * SDL_Surface handles produced by SDL_ttf are valid for sdl-kmp's
 * surface/renderer APIs.
 *
 * Consumers must initialize with initSdlTtfKmp() (not sdl-kmp's init) when
 * they use this package; both entry points instantiate the module, and two
 * instances would not share handles.
 *
 * Marshaling conventions (see ttf_wasm_shim.c):
 *   - scalars and handles are passed straight through as numbers;
 *   - strings are converted by cwrap;
 *   - functions with out-parameters write into a temporary heap buffer that
 *     is copied into the shared sdlKmpResultI32 slots (the last slot holds
 *     the success flag when the C function returns one).
 */

import { initSdlKmp as initSdlModule } from './sdl_kmp_glue.js';

let initPromise = null;

export function initSdlTtfKmp(options = {}) {
    if (initPromise === null) {
        initPromise = initSdlModule(options).then(
            (Module) => {
                installTtfBindings(Module);
                return Module;
            },
            (error) => {
                initPromise = null;
                throw error;
            },
        );
    }
    return initPromise;
}

/*
 * Alias for code that keeps sdl-kmp's initialization name. It initializes the
 * combined SDL3 + SDL_ttf module, NOT a separate SDL-only instance, so calling
 * both initSdlKmp() and initSdlTtfKmp() from this package is idempotent and
 * cannot create two heaps. Do not mix it with @enaium/sdl-kmp's own
 * initSdlKmp after this one: that would install sdl_kmp_* globals from a
 * different module and invalidate TTF handles.
 */
export const initSdlKmp = initSdlTtfKmp;

function installTtfBindings(Module) {
    const R32 = globalThis.sdlKmpResultI32;
    if (!R32) {
        throw new Error('sdl-kmp result buffers are missing: initSdlKmp did not run');
    }

    const C = Module.cwrap;
    const malloc = Module._malloc;
    const free = Module._free;

    /* Copies a C int array into the shared result slots and frees it. */
    const readInts = (ptr, count) => {
        const base = ptr >> 2;
        for (let i = 0; i < count; i++) {
            R32[i] = Module.HEAP32[base + i];
        }
        free(ptr);
    };

    /* Wraps a shim function whose last argument is an int out-array. The
     * values are copied to slots 0..count-1 and the return value to `flag`. */
    const outCall = (name, count, flag) => {
        const fn = Module['_' + name];
        return (...args) => {
            const ptr = malloc(count * 4);
            const ok = fn(...args, ptr);
            readInts(ptr, count);
            R32[flag] = ok;
            return ok;
        };
    };

    const setRet = (name) => {
        globalThis[name] = Module['_' + name];
    };
    const setCwrap = (name, ret, args) => {
        globalThis[name] = C(name, ret, args);
    };

    /* ------------------------------------------------------------------
     * Filesystem helper
     * ------------------------------------------------------------------ */
    /* Writes a byte array into the Emscripten filesystem, creating parent
     * directories. Apps use this to make fonts/documents available to
     * SDL_ttf's path-based APIs (TTF_OpenFont) without a preload step. */
    globalThis.sdlTtfKmpWriteFile = (path, bytes) => {
        if (!Module.FS) {
            throw new Error('Emscripten FS is not exported by the module');
        }
        const slash = path.lastIndexOf('/');
        if (slash > 0) {
            Module.FS.mkdirTree(path.substring(0, slash));
        }
        Module.FS.writeFile(path, bytes);
    };

    /* ------------------------------------------------------------------
     * Library
     * ------------------------------------------------------------------ */
    setRet('sdl_ttf_kmp_Init');
    setRet('sdl_ttf_kmp_Quit');
    setRet('sdl_ttf_kmp_WasInit');
    setRet('sdl_ttf_kmp_Version');
    globalThis.sdl_ttf_kmp_GetFreeTypeVersion = outCall('sdl_ttf_kmp_GetFreeTypeVersion', 3, 3);
    globalThis.sdl_ttf_kmp_GetHarfBuzzVersion = outCall('sdl_ttf_kmp_GetHarfBuzzVersion', 3, 3);

    /* ------------------------------------------------------------------
     * Fonts
     * ------------------------------------------------------------------ */
    setCwrap('sdl_ttf_kmp_OpenFont', 'number', ['string', 'number']);
    setRet('sdl_ttf_kmp_OpenFontIO');
    setRet('sdl_ttf_kmp_CopyFont');
    setRet('sdl_ttf_kmp_CloseFont');
    setRet('sdl_ttf_kmp_GetFontSize');
    setRet('sdl_ttf_kmp_SetFontSize');
    setRet('sdl_ttf_kmp_GetFontStyle');
    setRet('sdl_ttf_kmp_SetFontStyle');
    setRet('sdl_ttf_kmp_GetFontOutline');
    setRet('sdl_ttf_kmp_SetFontOutline');
    setRet('sdl_ttf_kmp_GetFontHinting');
    setRet('sdl_ttf_kmp_SetFontHinting');
    setRet('sdl_ttf_kmp_GetFontSDF');
    setRet('sdl_ttf_kmp_SetFontSDF');
    setRet('sdl_ttf_kmp_GetFontWrapAlignment');
    setRet('sdl_ttf_kmp_SetFontWrapAlignment');
    setRet('sdl_ttf_kmp_GetFontLineSkip');
    setRet('sdl_ttf_kmp_SetFontLineSkip');
    setRet('sdl_ttf_kmp_GetFontKerning');
    setRet('sdl_ttf_kmp_SetFontKerning');
    setRet('sdl_ttf_kmp_GetFontDirection');
    setRet('sdl_ttf_kmp_SetFontDirection');
    setRet('sdl_ttf_kmp_GetFontHeight');
    setRet('sdl_ttf_kmp_GetFontAscent');
    setRet('sdl_ttf_kmp_GetFontDescent');
    setCwrap('sdl_ttf_kmp_GetFontFamilyName', 'string', ['number']);
    setCwrap('sdl_ttf_kmp_GetFontStyleName', 'string', ['number']);
    setRet('sdl_ttf_kmp_FontIsFixedWidth');
    setRet('sdl_ttf_kmp_FontIsScalable');
    setRet('sdl_ttf_kmp_GetFontWeight');
    setRet('sdl_ttf_kmp_FontHasGlyph');

    globalThis.sdl_ttf_kmp_GetGlyphMetrics = outCall('sdl_ttf_kmp_GetGlyphMetrics', 5, 5);
    globalThis.sdl_ttf_kmp_GetGlyphKerning = outCall('sdl_ttf_kmp_GetGlyphKerning', 1, 1);

    const getStringSize = C('sdl_ttf_kmp_GetStringSize', 'number', ['number', 'string', 'number', 'number']);
    globalThis.sdl_ttf_kmp_GetStringSize = (font, text, wrapWidth) => {
        const ptr = malloc(8);
        const ok = getStringSize(font, text, wrapWidth, ptr);
        readInts(ptr, 2);
        R32[2] = ok;
        return ok;
    };

    const measureString = C('sdl_ttf_kmp_MeasureString', 'number', ['number', 'string', 'number', 'number']);
    globalThis.sdl_ttf_kmp_MeasureString = (font, text, maxWidth) => {
        const ptr = malloc(8);
        const ok = measureString(font, text, maxWidth, ptr);
        readInts(ptr, 2);
        R32[2] = ok;
        return ok;
    };

    setRet('sdl_ttf_kmp_AddFallbackFont');
    setRet('sdl_ttf_kmp_RemoveFallbackFont');
    setRet('sdl_ttf_kmp_ClearFallbackFonts');

    /* ------------------------------------------------------------------
     * Text engines
     * ------------------------------------------------------------------ */
    setRet('sdl_ttf_kmp_CreateRendererTextEngine');
    setRet('sdl_ttf_kmp_CreateSurfaceTextEngine');
    setRet('sdl_ttf_kmp_CreateGPUTextEngine');
    setRet('sdl_ttf_kmp_DestroyRendererTextEngine');
    setRet('sdl_ttf_kmp_DestroySurfaceTextEngine');
    setRet('sdl_ttf_kmp_DestroyGPUTextEngine');

    /* ------------------------------------------------------------------
     * Text objects
     * ------------------------------------------------------------------ */
    setCwrap('sdl_ttf_kmp_CreateText', 'number', ['number', 'number', 'string']);
    setRet('sdl_ttf_kmp_DestroyText');
    setCwrap('sdl_ttf_kmp_GetTextString', 'string', ['number']);
    setRet('sdl_ttf_kmp_GetTextNumLines');
    setRet('sdl_ttf_kmp_SetTextColor');
    globalThis.sdl_ttf_kmp_GetTextColor = outCall('sdl_ttf_kmp_GetTextColor', 4, 4);
    setRet('sdl_ttf_kmp_SetTextPosition');
    globalThis.sdl_ttf_kmp_GetTextPosition = outCall('sdl_ttf_kmp_GetTextPosition', 2, 2);
    setRet('sdl_ttf_kmp_SetTextWrapWidth');
    globalThis.sdl_ttf_kmp_GetTextWrapWidth = outCall('sdl_ttf_kmp_GetTextWrapWidth', 1, 1);
    setRet('sdl_ttf_kmp_TextWrapWhitespaceVisible');
    setRet('sdl_ttf_kmp_SetTextWrapWhitespaceVisible');
    globalThis.sdl_ttf_kmp_GetTextSize = outCall('sdl_ttf_kmp_GetTextSize', 2, 2);
    setRet('sdl_ttf_kmp_DrawRendererText');
    setRet('sdl_ttf_kmp_DrawSurfaceText');
    setCwrap('sdl_ttf_kmp_SetTextString', 'number', ['number', 'string']);
    setCwrap('sdl_ttf_kmp_AppendTextString', 'number', ['number', 'string']);
    setCwrap('sdl_ttf_kmp_InsertTextString', 'number', ['number', 'number', 'string']);
    setRet('sdl_ttf_kmp_DeleteTextString');
    globalThis.sdl_ttf_kmp_GetTextSubString = outCall('sdl_ttf_kmp_GetTextSubString', 8, 8);
    globalThis.sdl_ttf_kmp_GetTextSubStringForLine = outCall('sdl_ttf_kmp_GetTextSubStringForLine', 8, 8);
    globalThis.sdl_ttf_kmp_GetTextSubStringForPoint = outCall('sdl_ttf_kmp_GetTextSubStringForPoint', 8, 8);

    /* ------------------------------------------------------------------
     * Rendering
     * ------------------------------------------------------------------ */
    setCwrap('sdl_ttf_kmp_RenderTextSolid', 'number', ['number', 'string', 'number', 'number', 'number', 'number', 'number']);
    setCwrap('sdl_ttf_kmp_RenderTextShaded', 'number', ['number', 'string', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number']);
    setCwrap('sdl_ttf_kmp_RenderTextBlended', 'number', ['number', 'string', 'number', 'number', 'number', 'number', 'number']);
    setCwrap('sdl_ttf_kmp_RenderTextLCD', 'number', ['number', 'string', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number', 'number']);
    setRet('sdl_ttf_kmp_RenderGlyphSolid');
    setRet('sdl_ttf_kmp_RenderGlyphShaded');
    setRet('sdl_ttf_kmp_RenderGlyphBlended');
    setRet('sdl_ttf_kmp_RenderGlyphLCD');
    setRet('sdl_ttf_kmp_GetGlyphImage');
}
