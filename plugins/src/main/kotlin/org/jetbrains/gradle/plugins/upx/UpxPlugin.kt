package org.jetbrains.gradle.plugins.upx

import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.buildUpxUri
import org.jetbrains.gradle.plugins.xz
import java.io.File
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

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

        val upxExtension = extensions.create<Extension>("upx", "upx", versionProperty, executableProvider)

        val upxDownloadTask by tasks.registering {
            group = "upx"
            val destinationFile = buildDir
                .resolve("upx/downloads")
                .resolve(if (OperatingSystem.current().isWindows) "upx.zip" else "upx.tar.xz")
            inputs.property("version", versionProperty)
            outputs.file(destinationFile)
            onlyIf { !upxExtension.executableProvider.isPresent }
            doFirst {
                val platform = UpxSupportedOperatingSystems.current()
                    ?: error(
                        "Current OS \"${OperatingSystem.current()}\" is not supported." +
                                "The Upx Gradle plugin will be disabled."
                    )
                val request = HttpRequest.newBuilder()
                    .uri(buildUpxUri(versionProperty.get(), platform))
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
                    inputExecutable.set(outputFile)
                    doFirst {
                        val file = outputExecutable.get().asFile
                        if (file.exists()) file.delete()
                    }
                }
                tasks.register<Exec>("runCompressed${name.capitalize()}") {
                    group = "application"
                    dependsOn(compress)
                    doFirst { executable = compress.get().outputExecutable.get().asFile.absolutePath }
                }
            }
        }
    }
}