package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec
import org.jetbrains.gradle.plugins.docker.DockerPlugin

abstract class AbstractDockerExecTask : DefaultTask() {

    init {
        group = DockerPlugin.TASK_GROUP
        logging.captureStandardOutput(LogLevel.INFO)
        logging.captureStandardError(LogLevel.ERROR)
    }

    protected abstract fun provideArguments(): List<String>

    protected open fun ExecSpec.customizeExec() {
    }

    @TaskAction
    private fun execute() {
        project.exec {
            customizeExec()
            executable = "docker"
            args = provideArguments()
        }
    }
}
