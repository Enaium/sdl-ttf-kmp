plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    group = "cn.enaium.sdl"
    // -PsdlTtfVersion=1.0.0 overrides the default; the publish workflow uses
    // a release version (Maven Central does not accept SNAPSHOTs).
    version = findProperty("sdlTtfVersion") ?: "1.0-SNAPSHOT"
}
