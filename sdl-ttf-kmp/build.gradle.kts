import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

group = rootProject.group
version = rootProject.version

val ttfDir = rootProject.projectDir.resolve("includes/SDL_ttf")
val sdlDir = rootProject.projectDir.resolve("includes/SDL")

val hostOs = OperatingSystem.current()
val hostArch = System.getProperty("os.arch").lowercase()

// Apple targets build on macOS via Xcode; linuxX64 is built on Linux hosts;
// linuxArm64 is built on Linux aarch64 hosts or cross-compiled from x86_64
// with the aarch64-linux-gnu toolchain; mingwX64 is cross-compiled on Linux
// hosts with the x86_64-w64-mingw32 toolchain.
fun hasMingwCrossToolchain(): Boolean {
    val name = "x86_64-w64-mingw32-gcc"
    return System.getenv("PATH")?.split(File.pathSeparator).orEmpty().any { dir ->
        val f = File(dir, name)
        f.isFile && f.canExecute()
    }
}

fun hasAarch64CrossToolchain(): Boolean {
    val name = "aarch64-linux-gnu-gcc"
    return System.getenv("PATH")?.split(File.pathSeparator).orEmpty().any { dir ->
        val f = File(dir, name)
        f.isFile && f.canExecute()
    }
}

// Reads sdk.dir from local.properties (the standard place Gradle's Android
// plugins put the SDK path, e.g. /Users/<user>/Library/Android/sdk).
fun localSdkDir(): String? {
    val f = rootProject.file("local.properties")
    if (!f.isFile) return null
    return f.readLines()
        .firstOrNull { it.trimStart().startsWith("sdk.dir=") }
        ?.substringAfter('=')
        ?.trim()
}

// Locates an installed Android NDK, preferring the highest version under
// $ANDROID_HOME (or $ANDROID_SDK_ROOT, or local.properties' sdk.dir, or
// ~/Android/Sdk). androidNative targets cross-compile the SDL_ttf static
// library with this toolchain.
fun androidNdkPath(): String? {
    val home = System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: localSdkDir()
        ?: System.getProperty("user.home") + "/Android/Sdk"
    val ndkDir = File(home, "ndk")
    if (!ndkDir.isDirectory) return null
    return ndkDir.listFiles()
        ?.filter { it.isDirectory && it.name.matches(Regex("\\d+\\.\\d+\\.\\d+.*")) }
        ?.sortedBy { it.name }
        ?.lastOrNull()
        ?.absolutePath
}

fun canBuildNativeTarget(targetName: String): Boolean {
    return when {
        hostOs.isMacOsX && (
                targetName.startsWith("macos") ||
                        targetName.startsWith("ios") ||
                        targetName.startsWith("tvos")
                ) -> true

        hostOs.isLinux && targetName == "linuxX64" -> true
        hostOs.isLinux && targetName == "linuxArm64" &&
                (hostArch == "aarch64" || hostArch == "arm64" || hasAarch64CrossToolchain()) -> true
        hostOs.isLinux && targetName == "mingwX64" && hasMingwCrossToolchain() -> true
        targetName.startsWith("androidNative") && androidNdkPath() != null -> true
        else -> false
    }
}

fun resolveCmakeExecutable(): String {
    val exeName = if (OperatingSystem.current().isWindows) "cmake.exe" else "cmake"

    System.getenv("PATH")?.split(File.pathSeparator).orEmpty().forEach { dir ->
        val candidate = File(dir, exeName)
        if (candidate.isFile && candidate.canExecute()) return candidate.absolutePath
    }

    val extraPaths = listOf(
        "/opt/homebrew/bin",
        "/usr/local/bin",
        "/usr/bin",
        "/opt/local/bin",
    )
    extraPaths.forEach { dir ->
        val candidate = File(dir, exeName)
        if (candidate.isFile && candidate.canExecute()) return candidate.absolutePath
    }

    return exeName
}

val cmakeExecutable: String by lazy { resolveCmakeExecutable() }

// Minimum deployment targets. These match Kotlin/Native 2.4 defaults
// (macos 12.0, ios/tvos 15.0) so the SDL_ttf static library objects never
// exceed the final binary's minimum version.
val appleDeploymentTargets = mapOf(
    "macos" to "12.0",
    "ios" to "15.0",
    "tvos" to "15.0",
)

fun deploymentTargetFor(targetName: String): String {
    val prefix = listOf("macos", "ios", "tvos").first { targetName.startsWith(it) }
    return appleDeploymentTargets.getValue(prefix)
}

kotlin {
    // ==================== JVM ====================
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    // ==================== Native ====================
    macosArm64()
    macosX64()

    linuxX64()
    linuxArm64()
    androidNativeArm64()
    androidNativeArm32()
    androidNativeX64()
    androidNativeX86()

    mingwX64()

    // ==================== Wasm ====================
    // SDL3 and SDL_ttf are compiled to a single Emscripten module (see the
    // wasm/ directory); Kotlin/Wasm has no cinterop, so the wasmJs actuals
    // call flat shim functions through the JS glue shipped as the
    // @enaium/sdl-ttf-kmp npm package.
    wasmJs {
        browser()
    }

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    tvosArm64()
    tvosSimulatorArm64()

    // ==================== cinterop for all native targets ====================
    targets.withType<KotlinNativeTarget> {
        val targetName = this.name
        val canBuild = canBuildNativeTarget(targetName)

        compilations.getByName("main") {
            cinterops {
                create("ttf") {
                    defFile(project.file("src/nativeInterop/cinterop/ttf.def"))
                    includeDirs(
                        project.file("src/nativeInterop/cinterop"),
                        rootProject.file("includes/SDL/include"),
                        rootProject.file("includes/SDL_ttf/include"),
                    )
                    // crc32 intrinsics from the MinGW sysroot's <intrin.h>
                    // (and SDL headers) require SSE4.2 on x86_64.
                    if (targetName.endsWith("X64")) {
                        compilerOpts("-msse4.2")
                    }
                    if (canBuild) {
                        // Embed the per-target static library into the produced
                        // cinterop klib. Targets that can't be built on this host
                        // still get bindings (for klib publishing); the static
                        // library is built and embedded when building on the
                        // matching host.
                        val outputDir = layout.buildDirectory.dir("native/$targetName").get().asFile
                        extraOpts(
                            "-libraryPath", outputDir.absolutePath,
                            "-staticLibrary", "libSDL3_ttf.a",
                        )
                    }
                }
            }
            defaultSourceSet.kotlin.srcDir("src/nativeMain/kotlin")
        }
    }

    // ==================== Source sets ====================
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                // SDL3 bindings; exposed as `api` because the public API
                // (SDLSurface, SDLRenderer, SDLIOStream, ...) references the
                // cn.enaium.sdl types. The dependency is substituted with the
                // local sdl-kmp project via the composite includeBuild in
                // settings.gradle.kts.
                api(libs.sdl.kmp)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        jvmMain {
            dependencies {
                // SDL_ttf on the JVM comes from our own JNI shared library
                // (libsdl_ttf_jni), which references the SDL3 symbols of
                // libsdl_jni (pulled in transitively by the sdl-kmp
                // dependency). Bundle all five JNI artifacts so consumers get
                // the right native binary out of the box; TtfNativeLoader
                // picks one at runtime by os.name/os.arch.
                runtimeOnly(project(":ttf-jni-jvm-linux-x86_64"))
                runtimeOnly(project(":ttf-jni-jvm-linux-aarch64"))
                runtimeOnly(project(":ttf-jni-jvm-darwin-x86_64"))
                runtimeOnly(project(":ttf-jni-jvm-darwin-aarch64"))
                runtimeOnly(project(":ttf-jni-jvm-windows-x86_64"))
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }

        wasmJsMain {
            dependencies {
                // Typed arrays (Int32Array, Int8Array) used by the JS bridge.
                implementation(libs.kotlinx.browser)
            }
        }
    }
}

// Kotlin/Wasm tests would run the test entry before the Emscripten module
// exists: every wasmJs actual calls globals that initSdlTtfKmp() installs from
// the host page, which the Karma harness cannot inject. The browser example
// (:examples:ttf_renderer:browser, run headless under Node in CI) is the
// end-to-end wasm test instead.
tasks.matching { it.name == "wasmJsTest" || it.name == "wasmJsBrowserTest" }.configureEach {
    enabled = false
}

// ==================== Native: build static SDL_ttf library for each target ====================
val nativeDir = projectDir.resolve("native")

fun registerNativeBuildTasks(targetName: String, cmakeFlags: List<String> = emptyList()) {
    val outputDir = layout.buildDirectory.dir("native/$targetName").get().asFile
    val cmakeBuildDir = layout.buildDirectory.dir("cmake-$targetName").get().asFile

    val commonFlags = listOf(
        cmakeExecutable, nativeDir.absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
        "-DSDL_SOURCE_DIR=${sdlDir.absolutePath}",
        "-DSDLTTF_SOURCE_DIR=${ttfDir.absolutePath}",
        "-DSDLTTF_KMP_OUTPUT_DIR=${outputDir.absolutePath}",
    )

    val configureTask = tasks.register<Exec>("configureNative_$targetName") {
        onlyIf { canBuildNativeTarget(targetName) }
        doFirst {
            cmakeBuildDir.mkdirs()
            outputDir.mkdirs()
        }
        workingDir = cmakeBuildDir
        commandLine(commonFlags + cmakeFlags)
    }

    val buildTask = tasks.register<Exec>("buildNative_$targetName") {
        onlyIf { canBuildNativeTarget(targetName) }
        dependsOn(configureTask)
        workingDir = cmakeBuildDir
        commandLine(cmakeExecutable, "--build", ".", "--config", "Release")
    }

    tasks.matching {
        it.name.startsWith("cinteropTtf") &&
                it.name.endsWith(targetName.replaceFirstChar { c -> c.uppercase() })
    }.configureEach {
        dependsOn(buildTask)
        // The cinterop task's custom up-to-date check only watches headers and
        // the .def file; register the static library as an input so a rebuild
        // of SDL_ttf re-embeds it (otherwise stale archives would leak into the
        // published klib on incremental builds).
        inputs.file(outputDir.resolve("libSDL3_ttf.a"))
    }
}

if (hostOs.isMacOsX) {
    registerNativeBuildTasks(
        "macosArm64",
        listOf(
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("macosArm64")}",
        ),
    )
    registerNativeBuildTasks(
        "macosX64",
        listOf(
            "-DCMAKE_OSX_ARCHITECTURES=x86_64",
            "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("macosX64")}",
        ),
    )
    registerNativeBuildTasks(
        "iosArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_SYSROOT=iphoneos",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("iosArm64")}",
        ),
    )
    registerNativeBuildTasks(
        "iosX64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
            "-DCMAKE_OSX_ARCHITECTURES=x86_64",
            "-DCMAKE_OSX_SYSROOT=iphonesimulator",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("iosX64")}",
        ),
    )
    registerNativeBuildTasks(
        "iosSimulatorArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_SYSROOT=iphonesimulator",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("iosSimulatorArm64")}",
        ),
    )
    registerNativeBuildTasks(
        "tvosArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=tvOS",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_SYSROOT=appletvos",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("tvosArm64")}",
        ),
    )
    registerNativeBuildTasks(
        "tvosSimulatorArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=tvOS",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_SYSROOT=appletvsimulator",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=${deploymentTargetFor("tvosSimulatorArm64")}",
        ),
    )
} else if (hostOs.isLinux) {
    registerNativeBuildTasks("linuxX64")
    // linuxArm64 is cross-compiled on x86_64 hosts with the aarch64-linux-gnu
    // toolchain (canBuildNativeTarget gates on it).
    registerNativeBuildTasks(
        "linuxArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=Linux",
            "-DCMAKE_SYSTEM_PROCESSOR=aarch64",
            "-DCMAKE_C_COMPILER=aarch64-linux-gnu-gcc",
            "-DCMAKE_CXX_COMPILER=aarch64-linux-gnu-g++",
        ),
    )
    // Cross-compile the MinGW static library with the
    // x86_64-w64-mingw32 toolchain (canBuildNativeTarget gates on it).
    registerNativeBuildTasks(
        "mingwX64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=Windows",
            "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
            "-DCMAKE_C_COMPILER=x86_64-w64-mingw32-gcc",
            "-DCMAKE_CXX_COMPILER=x86_64-w64-mingw32-g++",
        ),
    )
}

// Android native targets cross-compile SDL_ttf with the NDK (see
// canBuildNativeTarget); the NDK can be used from any host OS.
androidNdkPath()?.let { ndk ->
    val toolchain = "$ndk/build/cmake/android.toolchain.cmake"
    val androidFlags = { abi: String, platform: String ->
        listOf(
            "-DCMAKE_TOOLCHAIN_FILE=$toolchain",
            "-DANDROID_ABI=$abi",
            "-DANDROID_PLATFORM=$platform",
            "-DANDROID_STL=c++_static",
        )
    }
    registerNativeBuildTasks("androidNativeArm64", androidFlags("arm64-v8a", "android-24"))
    registerNativeBuildTasks("androidNativeArm32", androidFlags("armeabi-v7a", "android-24"))
    registerNativeBuildTasks("androidNativeX64", androidFlags("x86_64", "android-24"))
    registerNativeBuildTasks("androidNativeX86", androidFlags("x86", "android-24"))
}

// ==================== wasm: build the combined SDL3 + SDL_ttf module ====================
// Kotlin/Wasm has no cinterop and cannot embed C libraries. SDL3 and SDL_ttf
// are therefore compiled to a single Emscripten module (sdl_wasm.js +
// sdl_wasm.wasm) that the wasmJs actuals drive through a JS glue layer (see
// src/wasmJsMain and the wasm/ directory).
//
// SDL3 must be part of the same module: the TTF-rendered SDL_Surface handles
// are consumed by the sdl-kmp renderer/surface bindings, which operate on the
// module's heap. The module also embeds sdl-kmp's SDL3 shim (vendored
// unchanged from sdl-kmp's wasm/ directory) so the sdl-kmp wasmJs bindings
// work against it as usual.

val wasmDir = projectDir.resolve("wasm")
val wasmOutDir = layout.buildDirectory.dir("wasm").get().asFile

fun emsdkPath(): String {
    System.getenv("EMSDK")?.takeIf { it.isNotBlank() }?.let { return it }
    providers.gradleProperty("wasm.emsdk").orNull?.takeIf { it.isNotBlank() }?.let { return it }
    val home = System.getProperty("user.home")
    listOf(File(home, "emsdk"), File(home, "sdk/emsdk"))
        .forEach { if (it.isDirectory()) return it.absolutePath }
    return ""
}

val wasmEmsdk = emsdkPath()
val wasmEmcc = File(wasmEmsdk, "upstream/emscripten/emcc")
val wasmToolchain = File(wasmEmsdk, "upstream/emscripten/cmake/Modules/Platform/Emscripten.cmake")

// Emscripten requires Python >= 3.10; the emsdk ships its own interpreter,
// which may not be on PATH (e.g. macOS ships Python 3.9 as /usr/bin/python3).
fun wasmEnv(): Map<String, String> {
    val pythonDir = File(wasmEmsdk, "python").listFiles()?.firstOrNull { it.isDirectory }
    return if (pythonDir != null) {
        mapOf("PATH" to "${pythonDir.absolutePath}/bin:${System.getenv("PATH")}")
    } else {
        emptyMap()
    }
}

tasks.register<Exec>("configureWasmTtf") {
    onlyIf { wasmEmsdk.isNotEmpty() }
    doFirst {
        File(wasmOutDir, "cmake").mkdirs()
        wasmOutDir.mkdirs()
    }
    workingDir = File(wasmOutDir, "cmake")
    environment(wasmEnv())
    // A toolchain file disables the generator's normal make discovery, so the
    // make program must be passed explicitly (emcmake does the same).
    val makeProgram = if (hostOs.isWindows) "nmake" else "/usr/bin/make"
    commandLine(
        cmakeExecutable, wasmDir.absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
        "-DCMAKE_TOOLCHAIN_FILE=${wasmToolchain.absolutePath}",
        "-DCMAKE_MAKE_PROGRAM=$makeProgram",
        "-DSDL_SOURCE_DIR=${sdlDir.absolutePath}",
        "-DSDLTTF_SOURCE_DIR=${ttfDir.absolutePath}",
        "-DWASM_OUTPUT_DIR=${wasmOutDir.absolutePath}",
    )
}

tasks.register<Exec>("buildWasmTtf") {
    onlyIf { wasmEmsdk.isNotEmpty() }
    dependsOn("configureWasmTtf")
    workingDir = File(wasmOutDir, "cmake")
    environment(wasmEnv())
    commandLine(cmakeExecutable, "--build", ".", "--config", "Release")
}

tasks.register<Exec>("linkWasmTtf") {
    onlyIf { wasmEmsdk.isNotEmpty() }
    dependsOn("buildWasmTtf")
    workingDir = wasmDir
    environment(wasmEnv())
    doFirst {
        wasmOutDir.mkdirs()
        // Export every sdl_kmp_* / sdl_ttf_kmp_* shim function. The prefix
        // alternation keeps sdl-kmp's and sdl-ttf-kmp's shims in one module.
        val exports = listOf(
            wasmDir.resolve("sdl_wasm_shim.c"),
            wasmDir.resolve("ttf_wasm_shim.c"),
        ).flatMap { file ->
            Regex("(?:sdl_kmp|sdl_ttf_kmp)_\\w+(?=\\()")
                .findAll(file.readText())
                .map { it.value }
                .toList()
        }.distinct().joinToString(",") { "_$it" }
        commandLine(
            wasmEmcc.absolutePath,
            wasmDir.resolve("sdl_wasm_shim.c").absolutePath,
            wasmDir.resolve("ttf_wasm_shim.c").absolutePath,
            File(wasmOutDir, "libSDL3_ttf.a").absolutePath,
            File(wasmOutDir, "libSDL3.a").absolutePath,
            "-I${sdlDir.absolutePath}/include",
            "-I${ttfDir.absolutePath}/include",
            "-o", File(wasmOutDir, "sdl_wasm.js").absolutePath,
            "-sMODULARIZE=1", "-sEXPORT_NAME=SDLModule", "-sEXPORT_ES6=1",
            "-sEXPORTED_FUNCTIONS=$exports,_malloc,_free",
            "-sEXPORTED_RUNTIME_METHODS=ccall,cwrap,UTF8ToString,stringToUTF8,lengthBytesUTF8,HEAPU8,HEAP32,HEAPU32,HEAP16,HEAPU16,HEAPF32,HEAPF64,HEAP8,FS,requestFullscreen",
            "-sALLOW_MEMORY_GROWTH=1", "-sINITIAL_MEMORY=67108864",
            "-sENVIRONMENT=web,worker,node", "-sFILESYSTEM=1", "-sMAX_WEBGL_VERSION=2", "--no-entry",
            "--js-library", wasmDir.resolve("emscripten_overrides.js").absolutePath,
        )
    }
    inputs.file(wasmDir.resolve("sdl_wasm_shim.c"))
    inputs.file(wasmDir.resolve("ttf_wasm_shim.c"))
    inputs.file(wasmDir.resolve("emscripten_overrides.js"))
    inputs.file(wasmDir.resolve("CMakeLists.txt"))
    // The archives are produced by buildWasmTtf; register them so changes in
    // SDL3/SDL_ttf re-link the module instead of keeping a stale one.
    inputs.file(File(wasmOutDir, "libSDL3.a"))
    inputs.file(File(wasmOutDir, "libSDL3_ttf.a"))
    outputs.file(File(wasmOutDir, "sdl_wasm.js"))
    outputs.file(File(wasmOutDir, "sdl_wasm.wasm"))
}

// ==================== wasm: npm package ====================
// The combined Emscripten module and the JS glue ship as an npm package
// (@enaium/sdl-ttf-kmp) instead of a Maven "assets" jar: web apps depend on
// it like any other npm package and `import { initSdlTtfKmp } from
// "@enaium/sdl-ttf-kmp"` before running the Kotlin/Wasm module, so no asset
// extraction step is involved.
//
// The package embeds sdl-kmp's SDL3 glue (sdl_kmp_glue.js, vendored from
// sdl-kmp) because the combined module replaces @enaium/sdl-kmp's SDL-only
// module: one Emscripten heap is required for TTF surfaces to interoperate
// with sdl-kmp's renderer/surface APIs. Apps using sdl-ttf-kmp on the web
// must initialize with initSdlTtfKmp instead of sdl-kmp's initSdlKmp.
//
// The package version is the Gradle version itself:
//   <kmp-module-version>-fixed.<SDL_ttf version>   (e.g. 1.0.0-fixed.3.2.2)
// which is valid npm semver, so Maven and npm publish the same coordinate
// string.

val ttfVersion: String by lazy {
    val header = ttfDir.resolve("include/SDL3_ttf/SDL_ttf.h").readText()
    fun macro(name: String): String =
        Regex("""#define\s+$name\s+(\d+)""").find(header)?.groupValues?.get(1)
            ?: error("$name is missing from SDL_ttf.h")
    "${macro("SDL_TTF_MAJOR_VERSION")}.${macro("SDL_TTF_MINOR_VERSION")}.${macro("SDL_TTF_MICRO_VERSION")}"
}

val npmPackageVersion: String by lazy {
    val version = rootProject.version.toString()
    val expectedSuffix = "-fixed.$ttfVersion"
    check(version.endsWith(expectedSuffix)) {
        "Gradle version '$version' must end with '$expectedSuffix' " +
            "(SDL_ttf.h reports $ttfVersion); update allprojects.version"
    }
    version
}

val wasmNpmDir = layout.buildDirectory.dir("npm").get().asFile

tasks.register("assembleWasmNpm") {
    group = "build"
    description = "Stages the @enaium/sdl-ttf-kmp npm package in build/npm."
    dependsOn("linkWasmTtf")
    inputs.file(wasmDir.resolve("ttf_kmp_glue.js"))
    inputs.file(wasmDir.resolve("sdl_kmp_glue.js"))
    // The version comes from this header and the Gradle version, so either
    // bump must re-stage.
    inputs.file(ttfDir.resolve("include/SDL3_ttf/SDL_ttf.h"))
    inputs.property("npmPackageVersion", npmPackageVersion)
    inputs.file(File(wasmOutDir, "sdl_wasm.js"))
    inputs.file(File(wasmOutDir, "sdl_wasm.wasm"))
    outputs.dir(wasmNpmDir)
    doLast {
        wasmNpmDir.deleteRecursively()
        wasmNpmDir.mkdirs()
        wasmDir.resolve("ttf_kmp_glue.js").copyTo(wasmNpmDir.resolve("ttf_kmp_glue.js"), overwrite = true)
        wasmDir.resolve("sdl_kmp_glue.js").copyTo(wasmNpmDir.resolve("sdl_kmp_glue.js"), overwrite = true)
        File(wasmOutDir, "sdl_wasm.js").copyTo(wasmNpmDir.resolve("sdl_wasm.js"), overwrite = true)
        File(wasmOutDir, "sdl_wasm.wasm").copyTo(wasmNpmDir.resolve("sdl_wasm.wasm"), overwrite = true)
        wasmNpmDir.resolve("package.json").writeText(
            """
            {
              "name": "@enaium/sdl-ttf-kmp",
              "version": "$npmPackageVersion",
              "description": "Emscripten-compiled SDL3 + SDL_ttf module and JS glue for the sdl-ttf-kmp wasmJs target",
              "license": "Zlib",
              "type": "module",
              "exports": {
                ".": "./ttf_kmp_glue.js",
                "./sdl_kmp_glue.js": "./sdl_kmp_glue.js",
                "./sdl_wasm.js": "./sdl_wasm.js",
                "./package.json": "./package.json"
              },
              "files": ["ttf_kmp_glue.js", "sdl_kmp_glue.js", "sdl_wasm.js", "sdl_wasm.wasm"],
              "homepage": "https://github.com/Enaium/sdl-ttf-kmp",
              "repository": {
                "type": "git",
                "url": "git+https://github.com/Enaium/sdl-ttf-kmp.git"
              }
            }
            """.trimIndent() + "\n",
        )
    }
}

tasks.register<Exec>("publishWasmNpm") {
    group = "publishing"
    description = "Publishes @enaium/sdl-ttf-kmp to npm (token: npm.publish.key)."
    dependsOn("assembleWasmNpm")
    workingDir = wasmNpmDir
    // npm is published from a workstation, not by CI (the publish workflow
    // only handles Maven Central). The token lives in the user-level Gradle
    // properties file (~/.gradle/gradle.properties); `-Pnpm.publish.key=<token>`
    // overrides it for one-off runs.
    val npmKey = providers.gradleProperty("npm.publish.key")
    doFirst {
        val key = npmKey.orNull?.takeIf { it.isNotBlank() }
            ?: throw GradleException(
                "npm.publish.key is not set: add it to ~/.gradle/gradle.properties " +
                    "or pass -Pnpm.publish.key=<token>",
            )
        // NODE_AUTH_TOKEN is expanded by npm through the environment, so the
        // token never lands in the package.json/tarball; .npmrc is on npm's
        // never-publish list anyway.
        environment("NODE_AUTH_TOKEN", key)
        wasmNpmDir.resolve(".npmrc").writeText(
            "//registry.npmjs.org/:_authToken=\${NODE_AUTH_TOKEN}\n",
        )
    }
    // The version is a semver prerelease (module version +
    // -fixed.<upstream>), so npm requires an explicit tag; `latest` keeps
    // `npm i @enaium/sdl-ttf-kmp` working.
    commandLine("npm", "publish", "--access", "public", "--tag", "latest")
}

// ==================== Publishing ====================
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "sdl-ttf-kmp",
        // null -> the plugin falls back to project.version
        version = null,
    )

    pom {
        name.set("sdl-ttf-kmp")
        description.set(
            "Kotlin Multiplatform bindings for SDL_ttf, built on top of sdl-kmp. " +
                    "JVM uses a self-contained JNI shared library that resolves its SDL3 " +
                    "symbols from sdl-kmp's libsdl_jni; native targets embed the statically " +
                    "compiled SDL_ttf library into the published klib; wasmJs drives a " +
                    "combined SDL3 + SDL_ttf Emscripten module shipped as the " +
                    "@enaium/sdl-ttf-kmp npm package.",
        )
        url.set("https://github.com/Enaium/sdl-ttf-kmp")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("Zlib")
                url.set("https://opensource.org/license/zlib")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("Enaium")
            }
        }

        scm {
            url.set("https://github.com/Enaium/sdl-ttf-kmp")
            connection.set("scm:git:git@github.com:Enaium/sdl-ttf-kmp.git")
            developerConnection.set("scm:git:git@github.com:Enaium/sdl-ttf-kmp.git")
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/Enaium/sdl-ttf-kmp/issues")
        }
    }
}
