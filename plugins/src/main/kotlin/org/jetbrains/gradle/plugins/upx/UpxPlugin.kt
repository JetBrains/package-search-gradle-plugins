@file:Suppress("UnstableApiUsage")

package org.jetbrains.gradle.plugins.upx

import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.*
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.konan.target.Family
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@Suppress("unused")
class UpxPlugin : Plugin<Project> {

    open class Extension(
        private val name: String,
        val version: Property<String>,
        val localUpxPath: Property<Path>,
        val upxExecutableProvider: Provider<Path>
    ) : Named {

        override fun getName() = name
    }

    override fun apply(target: Project): Unit = with(target) {

        val versionProperty = objects.property<String>()
            .convention("4.0.2")

        val localUpxPath = objects.property<Path>()

        val os = OperatingSystem.current()
        if (os.isMacOsX) {
            logger.lifecycle("Searching for installed upx...")
            val stdout = ByteArrayOutputStream()
            exec {
                standardOutput = stdout
                commandLine("whereis", "upx")
            }
            stdout.toString().lines()
                .firstOrNull() { it.startsWith("upx: ") }
                ?.removePrefix("upx: ")
                ?.split(" ")
                ?.firstOrNull()
                ?.let { Paths.get(it) }
                ?.also { logger.lifecycle("Found: ${it.absolutePathString()}") }
                ?.also { localUpxPath.convention(it) }
                ?: logger.lifecycle("upx executable not found.")
        }

        val upxDownloadTask by rootProject.tasks.getOrRegistering(DownloadTask::class) {
            group = "upx"
            outputFile = layout.buildDirectory
                .file("upx/downloads/" + if (os.isWindows) "upx.zip" else "upx.tar.xz")
            url = versionProperty.map {
                buildUpxUrl(
                    version = it,
                    platform = UpxSupportedOperatingSystems.current()
                        ?: error(
                            "Current OS \"$os\" is not supported." +
                                    "The Upx Gradle plugin will be disabled."
                        )
                )
            }
        }
        val unzipUpx by rootProject.tasks.getOrRegistering(Sync::class) {
            group = "upx"
            from(upxDownloadTask.map {
                if (os.isWindows) zipTree(it.outputFile) else tarTree(resources.xz(it.outputFile))
            })
            include { it.name == "upx" || it.name == "upx.exe" }
            eachFile { relativePath = RelativePath(true, name) }
            into(layout.buildDirectory.dir("upx/exec"))
        }

        val upxExecutable = when {
            localUpxPath.isPresent -> localUpxPath
            else -> unzipUpx.map { it.destinationDir.toPath().resolve("upx".suffixIf(os.isWindows) { ".exe" }) }
        }

        extensions.create<Extension>("upx", "upx", versionProperty, localUpxPath, upxExecutable)

        val compress by tasks.registering {
            group = "upx"
        }
        plugins.withId("org.graalvm.buildtools.native") {
            tasks.withType<BuildNativeImageTask> nativeBuild@{
                val compressTask = tasks.register<UpxTask>("compress${name.capitalize()}") {
                    dependsOn(this@nativeBuild)
                    inputExecutable = outputDirectory.flatMap { it.file(executableName) }
                    upxExecutableFile = upxExecutable
                }
                compress {
                    dependsOn(compressTask)
                }
                tasks.register<Exec>("runCompressed${name.capitalize()}") {
                    group = "application"
                    dependsOn(compressTask)
                    executable = compressTask.get().outputExecutable.get().asFile.absolutePath
                }
            }
        }
        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            tasks {
                withType<KotlinNativeLink> link@{
                    outputs.file(outputFile)
                    val compressTask = register<UpxTask>("compress${name.removePrefix("link")}") {
                        dependsOn(this@link)
                        inputExecutable = layout.file(outputFile)
                        upxExecutableFile = upxExecutable

                        onlyIf {
                            val isCompatible = when (binary.target.konanTarget.family) {
                                Family.OSX -> os.isMacOsX
                                Family.LINUX -> os.isLinux
                                Family.MINGW -> os.isWindows
                                else -> false
                            }
                            isCompatible && !sources.isEmpty
                        }
                    }
                    compress {
                        dependsOn(compressTask)
                    }
                    register<Exec>("runCompressed${compressTask.name.removePrefix("compress")}") {
                        dependsOn(compressTask)
                        group = "run"
                        executable = compressTask.get().outputExecutable.asFile.get().absolutePath
                    }
                }
            }
        }
    }
}