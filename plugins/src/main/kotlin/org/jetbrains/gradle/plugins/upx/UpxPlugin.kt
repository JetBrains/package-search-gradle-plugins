package org.jetbrains.gradle.plugins.upx

import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.DownloadTask
import org.jetbrains.gradle.plugins.buildUpxUrl
import org.jetbrains.gradle.plugins.xz
import java.io.ByteArrayOutputStream
import java.io.File

@Suppress("unused")
class UpxPlugin : Plugin<Project> {

    open class Extension(
        private val name: String,
        val version: Property<String>,
        val executableProvider: Property<File>
    ) : Named {

        override fun getName() = name
    }

    override fun apply(target: Project): Unit = with(target) {

        val versionProperty = objects.property<String>()
            .convention("4.0.2")

        val executableProvider = objects.property<File>()

        if (OperatingSystem.current().isMacOsX)
            executableProvider.convention(provider {
                val stdout = ByteArrayOutputStream()
                exec {
                    standardOutput = stdout
                    commandLine("whereis", "upx")
                }
                val upxPath = stdout.toString().lines()
                    .first { it.startsWith("upx: ") }
                    .removePrefix("upx: ")
                    .split(" ")
                    .first()
                File(upxPath)
            })

        val upxExtension = extensions.create<Extension>("upx", "upx", versionProperty, executableProvider)

        val upxDownloadTask by tasks.registering(DownloadTask::class) {
            group = "upx"
            outputFile.set(
                buildDir
                    .resolve("upx/downloads")
                    .resolve(if (OperatingSystem.current().isWindows) "upx.zip" else "upx.tar.xz")
            )
            url.set(versionProperty.map {
                buildUpxUrl(
                    it, UpxSupportedOperatingSystems.current()
                        ?: error(
                            "Current OS \"${OperatingSystem.current()}\" is not supported." +
                                    "The Upx Gradle plugin will be disabled."
                        )
                )
            })
        }

        tasks.register<Copy>("unzipUpx") {
            dependsOn(upxDownloadTask)
            group = "upx"
            if (!upxExtension.executableProvider.isPresent) {
                val file = upxDownloadTask.get().outputs.files.singleFile
                from(if (OperatingSystem.current().isWindows) zipTree(file) else tarTree(resources.xz(file)))
                include { it.name == "upx" || it.name == "upx.exe" }
                eachFile { relativePath = RelativePath(true, name) }
            } else from(upxExtension.executableProvider) {
                rename { if (OperatingSystem.current().isWindows) "upx.exe" else "upx" }
            }
            into("$buildDir/upx/exec")
        }
        plugins.withId("org.graalvm.buildtools.native") {
            tasks.withType<BuildNativeImageTask> nativeBuild@{
                val compress = tasks.register<UpxTask>("compress${name.capitalize()}") {
                    dependsOn(this@nativeBuild)
                    inputExecutable.set(outputDirectory.flatMap { it.file(executableName) })
                    doFirst {
                        val file = outputExecutable.get().asFile
                        if (file.exists()) file.delete()
                    }
                }
                tasks.register<Exec>("runCompressed${name.capitalize()}") {
                    group = "application"
                    dependsOn(compress)
                    executable = compress.get().outputExecutable.get().asFile.absolutePath
                }
            }
        }
    }
}

