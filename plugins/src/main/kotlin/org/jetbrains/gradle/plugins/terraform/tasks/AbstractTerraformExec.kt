package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.propertyWithDefault
import org.jetbrains.gradle.plugins.setValue
import org.jetbrains.gradle.plugins.terraform.TerraformPlugin
import java.io.File

abstract class AbstractTerraformExec : DefaultTask() {

    init {
        group = TerraformPlugin.TASK_GROUP
        @Suppress("LeakingThis")
        dependsOn(TerraformPlugin.TERRAFORM_EXTRACT_TASK_NAME)
    }

    @get:Input
    var arguments by project.objects.listProperty<String>()

    @get:InputDirectory
    var sourcesDirectory: File by project.objects.property()

    protected open fun ExecSpec.customizeExec() {
    }

    @TaskAction
    private fun execute(): ExecResult = project.exec {
        val terraformExecutable =
            project.tasks.named<TerraformExtract>(TerraformPlugin.TERRAFORM_EXTRACT_TASK_NAME).get().outputExecutable
        executable = terraformExecutable.absolutePath
        workingDir = sourcesDirectory
        args = arguments
        customizeExec()
    }
}
