package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.propertyWithDefault
import org.jetbrains.gradle.plugins.setValue
import java.io.File

open class DockerBuild : AbstractDockerTask() {

    @get:Input
    @get:Optional
    var tags by project.objects.listProperty<String>()

    @get:InputDirectory
    var contextFolder by project.objects.property<File>()

    @get:Input
    var buildArgs by project.objects.mapProperty<String, String?>()

    @TaskAction
    fun execute() {
        client.buildImageCmd().apply {
            this@DockerBuild.buildArgs.forEach { (k, v) -> withBuildArg(k, v) }
            withTags(this@DockerBuild.tags.toSet().also { println(it) })
            withBaseDirectory(contextFolder)
            withDockerfile(contextFolder.resolve("Dockerfile"))
        }.start().awaitCompletion()
    }
}
