plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "org.jetbrains.gradle"
    version = System.getenv("GITHUB_REF")?.substringAfterLast("/") ?: "0.0.1"
}
