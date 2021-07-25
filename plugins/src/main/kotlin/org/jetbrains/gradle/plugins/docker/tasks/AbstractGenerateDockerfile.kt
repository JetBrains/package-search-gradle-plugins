package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.jetbrains.gradle.plugins.docker.DockerPlugin
import org.jetbrains.gradle.plugins.propertyWithDefault
import java.io.File

abstract class AbstractGenerateDockerfile : DefaultTask() {

    init {
        group = DockerPlugin.TASK_GROUP
    }

    @get:OutputFile
    var outputDockerfile by project.objects.propertyWithDefault(File(project.buildDir, "dockerfiles/Dockerfile"))
}
