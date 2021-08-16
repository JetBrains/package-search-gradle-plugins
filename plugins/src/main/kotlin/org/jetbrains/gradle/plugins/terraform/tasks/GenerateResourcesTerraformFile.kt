package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.setValue
import org.jetbrains.gradle.plugins.terraform.tasks.GenerateResourcesTerraformFile.Companion.PATH_STRING_TEMPLATE
import org.jetbrains.gradle.plugins.writeText
import java.io.File

open class CopyTerraformResourceFileInModules : DefaultTask() {

    @get:InputFile
    var inputResFile by project.objects.property<File>()

    @get:InputDirectory
    var runtimeContextDir by project.objects.property<File>()

    @TaskAction
    fun generateFiles() {
        runtimeContextDir.walkBottomUp().filter {
            it.isDirectory && it.name != "resources"
                    && (it.listFiles() ?: emptyArray()).any { it.extension == "tf" }
        }
            .forEach { dir ->
                val doubleDotsCount = dir.absolutePath.removePrefix(runtimeContextDir.absolutePath)
                    .split(File.separator).size - 1
                val pathName = buildString {
                    if (doubleDotsCount == 0) append("./")
                    else repeat(doubleDotsCount) { append("../") }
                    append("resources/")
                }
                println("""
                    From: ${dir.absolutePath}
                    to:   ${runtimeContextDir.absolutePath}
                    res : $pathName
                    _______________
                """.trimIndent())

                dir.resolve(inputResFile.name).writeText(inputResFile.readText().replace(PATH_STRING_TEMPLATE, pathName))
            }
    }

}

open class GenerateResourcesTerraformFile : DefaultTask() {

    companion object {
        internal const val PATH_STRING_TEMPLATE = "%%%PATH_STRING%%%"
    }

    @get:InputDirectory
    var resourcesDirectory by project.objects.property<File>()

    @get:OutputFile
    var outputResourceModuleFile by project.objects.property<File>()

    @TaskAction
    fun writeFiles() {
        if (outputResourceModuleFile.exists().not()) outputResourceModuleFile.createNewFile()
        outputResourceModuleFile.writeText {
            appendLine(
                """
                variable "resources" {
                  default = {
            """.trimIndent()
            )
            printDirectory(resourcesDirectory)
            appendLine(
                """
              }
            """.trimIndent()
            )
        }
    }

    private fun File.distanceFromRoot() = absolutePath.removePrefix(resourcesDirectory.absolutePath)
        .split(File.separator)
        .size - 1

    private fun StringBuilder.printDirectory(directory: File) {
        val dirContent: List<File> = directory.listFiles()?.toList() ?: emptyList()
        dirContent.filter { it.isFile }.forEach { file ->
            val resName = file.name.replace(".", "-")
            val relativePath = file.relativeTo(resourcesDirectory).path.replace(File.separator, "/")
            val resPath = "$PATH_STRING_TEMPLATE${relativePath}"
            repeat(file.distanceFromRoot() + 1) { append("  ") }
            appendLine("$resName = \"$resPath\"")
        }
        dirContent.filter { it.isDirectory }.forEach { dir ->
            val resName = dir.name.replace(".", "-")
            val resPath = "$PATH_STRING_TEMPLATE${dir.name}"
            repeat(dir.distanceFromRoot() + 1) { append("  ") }
            appendLine("$resName-dir = \"$resPath\"")
            repeat(dir.distanceFromRoot() + 1) { append("  ") }
            appendLine("$resName = {")
            printDirectory(dir)
        }
        repeat(directory.distanceFromRoot() + 1) { append("  ") }
        appendLine("}")
    }
}