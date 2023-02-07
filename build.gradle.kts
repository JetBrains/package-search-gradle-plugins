plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.graalvm.buildtools.native) apply false
}

allprojects {
    group = "org.jetbrains.gradle"
    version = System.getenv("GITHUB_REF")?.substringAfterLast("/") ?: "0.0.1"
}
