package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue

open class DockerPush : AbstractDockerTask() {

    @get:Input
    @get:Optional
    var imageTag by project.objects.property<String>()

    fun execute() {
        client.pushImageCmd(imageTag).start().awaitCompletion()
    }
}
