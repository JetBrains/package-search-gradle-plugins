plugins {
    application
    alias(libs.plugins.graalvm.buildtools.native)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
//    id("org.jetbrains.gradle.upx")
}

application {
    mainClass.set("org.jetbrains.gradle.upx.example.MainKt")
}

graalvmNative {
    agent {

    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.apache)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.logback.classic)
    implementation(libs.ktor.serialization.kotlinx.json)
}
