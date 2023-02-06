package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jetbrains.gradle.plugins.docker.DockerPlugin

abstract class AbstractDockerExecTask : DefaultTask() {

    init {
        group = DockerPlugin.TASK_GROUP
    }

    protected abstract fun provideArguments(): List<String>

    @TaskAction
    private fun execute() {
        project.exec {
            executable = "docker"
            args = provideArguments()
        }
    }
}
