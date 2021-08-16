package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
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
        }.forEach { dir ->
            dir.resolve(inputResFile.name).writeText(
                inputResFile.readText().replace(GenerateResourcesTerraformFile.PATH_STRING_TEMPLATE, "./resources/")
            )
        }
    }
}