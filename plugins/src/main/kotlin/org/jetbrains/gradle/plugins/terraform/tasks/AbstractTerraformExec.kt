package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.jetbrains.gradle.plugins.terraform.TerraformPlugin
import java.io.File
import java.nio.file.Paths

abstract class AbstractTerraformExec : DefaultTask() {

    companion object {
        private fun getAWSCredentialsEnv() =
            System.getenv("AWS_SHARED_CREDENTIALS_FILE")
                ?: Paths.get(
                    System.getProperty("user.home"),
                    ".aws", "credentials"
                ).toAbsolutePath().toString()
    }

    init {
        group = TerraformPlugin.TASK_GROUP
        @Suppress("LeakingThis")
        dependsOn(TerraformPlugin.TERRAFORM_EXTRACT_TASK_NAME)
        logging.captureStandardOutput(LogLevel.INFO)
    }

    @get:InputDirectory
    var sourcesDirectory by project.objects.property<File>()

    abstract var dataDir: File

    @Internal
    protected abstract fun getTerraformArguments(): List<String>

    protected open fun ExecSpec.customizeExec() {}

    @TaskAction
    private fun execute(): ExecResult = project.exec {
        val terraformExecutable = project.tasks
            .named<TerraformExtract>(TerraformPlugin.TERRAFORM_EXTRACT_TASK_NAME)
            .get().outputExecutable
        executable = terraformExecutable.absolutePath
        workingDir = sourcesDirectory
        args = getTerraformArguments()
        environment = mapOf(
            "TF_DATA_DIR" to dataDir.absolutePath,
            "AWS_SHARED_CREDENTIALS_FILE" to getAWSCredentialsEnv()
        )
        customizeExec()
    }
}
