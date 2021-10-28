package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue

interface DockerPushSpec : Task {
    @get:Input
    var imageTag: String
}

open class DockerPush : AbstractDockerTask(), DockerPushSpec {

    override var imageTag by project.objects.property<String>()

    @TaskAction
    fun execute() {
        client.pushImageCmd(imageTag).start().awaitCompletion()
    }
}

open class DockerExecPush : AbstractDockerExecTask(), DockerPushSpec {

    override var imageTag by project.objects.property<String>()

    override fun provideArguments() = listOf("push", imageTag)
}
