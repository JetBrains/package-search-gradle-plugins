package org.jetbrains.gradle.plugins.upx

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.absolutePathString

open class UnzipUpx

open class UpxTask : DefaultTask() {

    init {
        group = "upx"
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
        .convention(inputExecutable.flatMap {
            val name = buildString {
                append(it.asFile.nameWithoutExtension)
                append("-compressed")
                if (OperatingSystem.current().isWindows) append(".exe")
            }
            val pathString = it.asFile.toPath().parent.resolve(name).absolutePathString()
            project.layout.buildDirectory.file(pathString)
        })

    @get:Input
    val command = project.objects.property<Command>()
        .convention(Command.COMPRESS)

    @get:Input
    val logLevel = project.objects.property<LogLevel>()
        .convention(LogLevel.NORMAL)

    @get:Input
    val exact = project.objects.property<Boolean>()
        .convention(false)

    @get:Input
    val overlay = project.objects.property<Overlay>()
        .convention(Overlay.COPY)

    @get:Input
    val compressionLevel = project.objects.property<CompressionLevel>()
        .convention(CompressionLevel.Best)

    @get:Input
    @get:Optional
    val bruteLevel = project.objects.property<BruteLevel>()

    @get:Input
    val additionalOptions = project.objects.listProperty<String>()
        .convention(emptyList())

    @get:InputFile
    val upxExecutableFile = project.objects.property<Path>()

    @TaskAction
    fun execute() {
        outputExecutable.get().asFile.delete()
        project.exec {
            executable = upxExecutableFile.get().absolutePathString()
            args(buildList<String> {
                command.get().command?.let { add(it) }
                add("-o")
                add(outputExecutable.get().asFile.absolutePath)
                logLevel.get().command?.let { add(it) }
                if (exact.get()) add("--exact")
                add("--overlay=${overlay.get().name.toLowerCase()}")
                add(compressionLevel.get().command)
                bruteLevel.orNull?.let { add(it.command) }
                addAll(additionalOptions.get())
                add(inputExecutable.get().asFile.absolutePath)
            })

        }
    }
}