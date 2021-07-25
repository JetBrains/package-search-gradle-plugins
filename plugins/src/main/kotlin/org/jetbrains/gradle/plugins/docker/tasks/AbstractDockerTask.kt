package org.jetbrains.gradle.plugins.docker.tasks

import com.github.dockerjava.api.DockerClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.jetbrains.gradle.plugins.docker.DockerPlugin

abstract class AbstractDockerTask : DefaultTask() {

    init {
        group = DockerPlugin.TASK_GROUP
    }

    @get:Input
    var client: DockerClient by project.objects.property()

}
