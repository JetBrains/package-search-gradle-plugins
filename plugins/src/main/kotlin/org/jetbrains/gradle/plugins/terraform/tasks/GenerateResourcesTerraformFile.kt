package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.setValue
import org.jetbrains.gradle.plugins.writeText
import java.io.File

open class GenerateResourcesTerraformFile : DefaultTask() {

    @get:InputDirectory
    var resourcesDirectory by project.objects.property<File>()

    @get:OutputFile
    var outputResourceModuleFile by project.objects.property<File>()

    @TaskAction
    fun writeFiles() {
        outputResourceModuleFile.writeText {
            val upperPath = "./resources/"
            appendLine(
                """
                variable "resources" {
                    default = {
            """.trimIndent()
            )
            val directories = mutableListOf(resourcesDirectory)
            fun File.distanceFromRoot() = absolutePath.removePrefix(resourcesDirectory.absolutePath)
                .split(File.separator)
                .size - 1
            while (directories.isNotEmpty()) {
                val dirContent: List<File> = directories.removeAt(0).listFiles()?.toList() ?: emptyList()
                dirContent.filter { it.isFile }.forEach { file ->
                    val resName = file.name.replace(".", "_")
                    val resPath = "$upperPath${file.name}"
                    repeat(file.distanceFromRoot() + 2) { append("    ") }
                    appendLine("$resName = \"$resPath\"")
                }
                val contentDirectories = dirContent.filter { it.isDirectory }.forEach {
                    
                }

            }
        }
    }
}

sealed class FileElement {

}