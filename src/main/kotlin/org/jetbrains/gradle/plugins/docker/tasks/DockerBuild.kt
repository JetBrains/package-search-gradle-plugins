package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.setValue
import java.io.File

open class DockerBuild : AbstractDockerTask(), DockerBuildSpec {

    override var tags by project.objects.listProperty<String>()

    override var contextFolder by project.objects.property<File>()

    override var buildArgs by project.objects.mapProperty<String, String?>()

    @TaskAction
    fun execute() {
        client.buildImageCmd().apply {
            this@DockerBuild.buildArgs.forEach { (k, v) -> withBuildArg(k, v.toString()) }
            withTags(this@DockerBuild.tags.toSet().also { println(it) })
            withBaseDirectory(contextFolder)
            withDockerfile(contextFolder.resolve("Dockerfile"))
        }.start().awaitCompletion()
    }
}
