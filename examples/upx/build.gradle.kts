@file:Suppress("UnstableApiUsage")

plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.kotlin.plugin.serialization)
    id("org.jetbrains.gradle.upx")
}

upx {
    version = "4.1.0"
}

kotlin {
    configure(listOf(macosArm64(), macosX64(), mingwX64())) {
        binaries {
            executable {
                entryPoint = "org.jetbrains.gradle.upx.example.main"
            }
        }
    }
    linuxX64 {
        binaries {
            executable {
                entryPoint = "org.jetbrains.gradle.upx.example.main"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
        val macosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val macosArm64Main by getting {
            dependsOn(macosMain)
        }
        val macosX64Main by getting {
            dependsOn(macosMain)
        }
        named("mingwX64Main") {
            dependencies {
                implementation(libs.ktor.client.winhttp)
            }
        }
        named("linuxX64Main") {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
    }
}

