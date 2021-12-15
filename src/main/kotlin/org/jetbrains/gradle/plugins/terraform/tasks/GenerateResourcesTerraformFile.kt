package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.jetbrains.gradle.plugins.writeText
import java.io.File

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
            val resName = buildString {
                append(file.nameWithoutExtension.map { if (it.isLetterOrDigit()) it else '-' }.joinToString(""))
                if (file.extension.isNotEmpty()) append("_${file.extension}")
            }
            val relativePath = file.relativeTo(resourcesDirectory).path.replace(File.separator, "/")
            val resPath = "$PATH_STRING_TEMPLATE${relativePath}"
            val tabsCount = file.distanceFromRoot() + 1
            appendLine(tabsCount, "$resName = {")
            appendLine(tabsCount + 1, "path = \"$resPath\"")
            appendLine(tabsCount + 1, "name = \"${file.name}\"")
            appendLine(tabsCount + 1, "name-without-extension = \"${file.nameWithoutExtension}\"")
            appendLine(tabsCount + 1, "ext = \"${file.extension}\"")
            appendLine(tabsCount, "}")
        }
        dirContent.filter { it.isDirectory }.forEach { dir ->
            val resName = dir.name.replace(".", "-")
            val resPath = "$PATH_STRING_TEMPLATE${dir.name}"
            val tabsCount = dir.distanceFromRoot() + 1
            appendLine(tabsCount, "$resName-dir = \"$resPath\"")
            appendLine(tabsCount, "$resName = {")
            printDirectory(dir)
        }
        repeat(directory.distanceFromRoot() + 1) { append("  ") }
        appendLine("}")
    }

    private fun StringBuilder.appendLine(tabs: Int, text: String) {
        repeat(tabs) { append("  ") }
        appendLine(text)
    }
}