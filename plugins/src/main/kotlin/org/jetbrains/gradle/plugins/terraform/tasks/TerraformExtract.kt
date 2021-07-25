package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setValue
import org.jetbrains.gradle.plugins.terraform.TerraformPlugin
import java.io.File

open class TerraformExtract : DefaultTask() {

    init {
        group = TerraformPlugin.TASK_GROUP
    }

    @get:InputFile
    var sourceZip by project.objects.property<File>()

    @get:OutputFile
    var outputExecutable by project.objects.property<File>()

    @TaskAction
    fun extract() {
        project.zipTree(sourceZip).single { it.nameWithoutExtension == "terraform" }
            .copyTo(outputExecutable, true)
        if (OperatingSystem.current().isUnix) outputExecutable.setExecutable(true)
    }
}
