
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// Browser (wasmJs) runner for the sdl_ttf demo. SDL3 and SDL_ttf are compiled
// to one Emscripten module (sdl_wasm.js, built by :sdl-ttf-kmp:linkWasmTtf)
// that this module loads before the Kotlin code runs; the wasmJs actuals in
// sdl-ttf-kmp drive it through the JS glue (sdl-ttf-kmp/wasm/ttf_kmp_glue.js).
kotlin {
    wasmJs {
        binaries.executable()
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":sdl-ttf-kmp"))
                implementation(project(":examples:ttf_renderer"))
            }
        }
    }
}

// ==================== web assets ====================
// The Emscripten module and the JS glue are consumed as the
// @enaium/sdl-ttf-kmp npm package: this module's package.json points the
// dependency at the package staged by :sdl-ttf-kmp:assembleWasmNpm, so the
// build works without registry access (an app using the published package
// depends on "^1.0.1-fixed.3.2.2" instead of the file: path).
//
// The wasmJs webpack distribution requires npm, which is not always
// available, so assemble a self-contained static app directory instead:
//   build/browserApp/
//     index.html                          (entry page; import map -> package)
//     fonts/DejaVuSans.ttf                (written into the module FS)
//     node_modules/@enaium/sdl-ttf-kmp/   (installed package: glue + module)
//     browser-node-test.mjs               (headless Node runner, bare import)
//     *.mjs/*.wasm                        (the Kotlin/Wasm executable output)
// Serve it with any static file server (e.g. `python3 -m http.server`).

val ttfProject = project(":sdl-ttf-kmp")
val kotlinWasmOutput = layout.buildDirectory.dir("compileSync/wasmJs/main/developmentExecutable/kotlin").get().asFile
val installedWasmPackage = layout.projectDirectory.dir("node_modules/@enaium/sdl-ttf-kmp")

val npmInstall = tasks.register<Exec>("npmInstall") {
    description = "Installs the example's npm dependencies (@enaium/sdl-ttf-kmp)."
    dependsOn(ttfProject.tasks.named("assembleWasmNpm"))
    workingDir = layout.projectDirectory.asFile
    commandLine("npm", "install", "--no-audit", "--no-fund")
    inputs.file(layout.projectDirectory.file("package.json"))
    outputs.dir(layout.projectDirectory.dir("node_modules"))
}

val copyWebAssets = tasks.register<Copy>("copyWebAssets") {
    dependsOn("compileDevelopmentExecutableKotlinWasmJs")
    dependsOn(npmInstall)
    from("src/wasmJsMain/resources/web") {
        include("**")
    }
    from(layout.projectDirectory) {
        include("browser-node-test.mjs")
    }
    // npm installs file: dependencies as a symlink; copy the real package
    // contents so the static app resolves it through the import map (browser)
    // and through node_modules (Node).
    from(provider { installedWasmPackage.asFile.canonicalFile }) {
        into("node_modules/@enaium/sdl-ttf-kmp")
    }
    from(project(":examples:ttf_renderer").projectDir.resolve("fonts")) {
        include("DejaVuSans.ttf")
        into("fonts")
    }
    from(kotlinWasmOutput) {
        include("**")
    }
    into(layout.buildDirectory.dir("browserApp"))
}
