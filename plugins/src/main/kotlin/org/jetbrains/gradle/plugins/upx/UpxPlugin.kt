package org.jetbrains.gradle.plugins.upx

import org.graalvm.buildtools.gradle.NativeImagePlugin
import org.graalvm.buildtools.gradle.NativeImagePlugin.NATIVE_COMPILE_TASK_NAME
import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.buildUpxLink
import org.jetbrains.gradle.plugins.xz
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class UpxPlugin : Plugin<Project> {

    open class Extension(
        private val name: String,
        val version: Property<String>
    ) : Named {
        override fun getName() = name
    }

    override fun apply(target: Project): Unit = with(target) {
        val currentOs = UpxSupportedOperatingSystems.current()
        if (currentOs == null) {
            logger.warn(
                "Current OS \"${OperatingSystem.current()}\" is not supported." +
                        "The Upx Gradle plugin will be disabled."
            )
            return@with
        }
        val versionProperty = objects.property<String>()
            .apply { set("4.0.2") }

        extensions.create<Extension>("upx", "upx", versionProperty)

        val upxDownloadTask by tasks.registering {
            group = "upx"
            val destinationFile = buildDir
                .resolve("upx/downloads")
                .resolve(if (OperatingSystem.current().isWindows) "upx.zip" else "upx.tar.xz")
            inputs.property("version", versionProperty)
            outputs.file(destinationFile)
            doFirst {
                val request = HttpRequest.newBuilder()
                    .uri(URI(buildUpxLink(versionProperty.get(), currentOs)))
                    .GET()
                    .build()
                val body = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofInputStream())
                    .body()
                destinationFile.outputStream()
                    .use { it.write(body.readAllBytes()) }
            }
        }
        tasks.register<Copy>("unzipUpx") {
            dependsOn(upxDownloadTask)
            group = "upx"
            val file = upxDownloadTask.get().outputs.files.singleFile
            from(if (OperatingSystem.current().isWindows) zipTree(file) else tarTree(resources.xz(file)))
            include { it.name == "upx" || it.name == "upx.exe" }
            eachFile { relativePath = RelativePath(true, name) }
            into("$buildDir/upx/exec")
        }
        plugins.withId("org.graalvm.buildtools.native") {
            val graalvm = extensions.getByType<GraalVMExtension>()
            graalvm.binaries.all {
                val buildNativeImageTaskName =
                    if ("main" == name) NATIVE_COMPILE_TASK_NAME else "$name${NATIVE_COMPILE_TASK_NAME.capitalize()}"
                tasks.register<UpxTask>("compress${buildNativeImageTaskName.capitalize()}") {
                    val buildNativeImageTask = tasks.named<BuildNativeImageTask>(buildNativeImageTaskName)
                    dependsOn(buildNativeImageTask)
                    inputExecutable.convention(buildNativeImageTask.map { it.outputFile.get() })
                }
            }
//            tasks.withType<BuildNativeImageTask> {
//                val me = this
//                tasks.register("compress${name.capitalize()}") {
//                    dependsOn(this)
//
//                }
//            }
        }
    }
}