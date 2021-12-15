package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.jetbrains.gradle.plugins.docker.DockerPlugin
import org.jetbrains.gradle.plugins.property
import java.io.File

abstract class AbstractGenerateDockerfile : DefaultTask() {

    init {
        group = DockerPlugin.TASK_GROUP
    }

    @get:OutputFile
    var outputDockerfile: File by project.objects
        .property(project.run { file("$buildDir/dockerfiles/Dockerfile") })

}
