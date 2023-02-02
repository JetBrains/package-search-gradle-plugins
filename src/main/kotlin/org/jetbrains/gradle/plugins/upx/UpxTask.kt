package org.jetbrains.gradle.plugins.upx

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.*
import java.io.Serializable

open class UpxTask : DefaultTask() {

    init {
        group = "upx"
        if ("unzipUpx" in project.tasks.names) dependsOn("unzipUpx")
        else throw error(
            "the Upx Gradle Plugin has not been applied. " +
                    "Please apply the plugin before creating an UpxTask"
        )
    }

    enum class Command(val command: String? = null) {
        COMPRESS, DECOMPRESS("-d"), TEST("-t"), LIST("-l")
    }

    enum class LogLevel(val command: String? = null) {
        NORMAL, NO_WARNINGS("-q"), NO_ERRORS("-qq"), OFF("-qqq")
    }

    enum class Overlay {
        COPY, STRIP, SKIP
    }

    sealed class CompressionLevel : Serializable {

        abstract val command: String

        data class Number(val amount: Int) : CompressionLevel() {
            override val command = "-$amount"
        }

        object Best : CompressionLevel() {
            override val command = "--best"
        }
    }

    enum class BruteLevel(val command: String) {
        BRUTE("--brute"), ULTRA_BRUTE("--ultra-brute")
    }

    @get:InputFile
    val inputExecutable = project.objects.fileProperty()

    @get:OutputFile
    @get:Optional
    val outputExecutable = project.objects.fileProperty()
        .apply {
            convention(inputExecutable.flatMap {
                project.layout.buildDirectory
                    .file("upx/outputs/${it.asFile.nameWithoutExtension}.upx${it.asFile.extension}")
            })
        }

    @get:Input
    var command = Command.COMPRESS

    @get:Input
    var logLevel = LogLevel.NORMAL

    @get:Input
    var isExact = false

    @get:Input
    var overlay = Overlay.COPY

    @get:Input
    var compressionLevel: CompressionLevel = CompressionLevel.Best

    @get:Input
    @get:Optional
    var bruteLevel: BruteLevel? = null

    @get:Input
    var additionalOptions = mutableListOf<String>()

    @TaskAction
    fun execute() {
        project.exec {

            executable = project.tasks
                .named<UpxTask>("upxTask")
                .get()
                .outputs
                .files
                .singleFile
                .walkTopDown()
                .first { it.isFile && it.name == if (OperatingSystem.current().isWindows) "upx.exe" else "upx" }
                .absolutePath

            args(buildList<String> {
                command.command?.let { add(it) }
                add("-o")
                add(outputExecutable.get().asFile.absolutePath)
                logLevel.command?.let { add(it) }
                if (isExact) add("--exact")
                add("--overlay=${overlay.name.toLowerCase()}")
                add(compressionLevel.command)
                bruteLevel?.let { add(it.command) }
                addAll(additionalOptions)
                add(inputExecutable.get().asFile.absolutePath)
            })

        }
    }
}