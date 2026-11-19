pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()

        // The Kotlin/Wasm toolchain downloads Node.js and Binaryen through
        // Ivy repositories added by the Kotlin Gradle plugin to the project.
        // PREFER_SETTINGS discards project-level repositories, so declare the
        // same repositories here.
        ivy("https://nodejs.org/dist") {
            name = "Node.js distributions"
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy("https://github.com/WebAssembly/binaryen/releases/download") {
            name = "Binaryen releases"
            patternLayout {
                artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            name = "Yarn releases"
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]).[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
    }
}

rootProject.name = "sdl-ttf-kmp"

// sdl-kmp provides the SDL3 bindings sdl-ttf-kmp builds on; the published
// artifacts (cn.enaium.sdl:sdl-kmp:1.0.14-fixed.3.4.18) are resolved from the
// repositories declared above (see the `api` dependency in
// sdl-ttf-kmp/build.gradle.kts).

include(":sdl-ttf-kmp")

include(":examples:ttf_renderer")
include(":examples:ttf_renderer:browser")
include(":examples:ttf_gpu")

// Per-OS/arch JNI artifacts that bundle the prebuilt libsdl_ttf_jni shared
// library as a classpath resource. The TTF JNI library references the SDL3
// symbols exported by libsdl_jni (from the sdl-kmp project), so the matching
// sdl-kmp JNI artifact must be on the classpath too (sdl-kmp pulls it in
// automatically). TtfNativeLoader extracts the matching one at runtime.
listOf(
    "linux-x86_64",
    "linux-aarch64",
    "darwin-x86_64",
    "darwin-aarch64",
    "windows-x86_64",
).forEach { classifier ->
    val name = ":ttf-jni-jvm-$classifier"
    include(name)
    project(name).projectDir = file("jni/jvm/$classifier")
}
