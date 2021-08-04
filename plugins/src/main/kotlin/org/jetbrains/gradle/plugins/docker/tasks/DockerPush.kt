package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue

interface DockerPushSpec : Task {
    var imageTag: String
}

open class DockerPush : AbstractDockerTask(), DockerPushSpec {

    @get:Input
    @get:Optional
    override var imageTag by project.objects.property<String>()

    @TaskAction
    fun execute() {
        client.pushImageCmd(imageTag).start().awaitCompletion()
    }
}

open class DockerExecPush : AbstractDockerExecTask(), DockerPushSpec {

    @get:Input
    @get:Optional
    override var imageTag by project.objects.property<String>()

    override fun provideArguments() = listOf("push", imageTag)
}
