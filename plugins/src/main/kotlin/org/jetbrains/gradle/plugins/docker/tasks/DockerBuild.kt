package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.propertyWithDefault
import org.jetbrains.gradle.plugins.setValue
import java.io.File

open class DockerBuild : AbstractDockerTask() {

    init {
        outputs.cacheIf { false }
    }

    @get:Input
    @get:Optional
    var tags by project.objects.listProperty<String>()

    @get:InputDirectory
    var contextFolder by project.objects.property<File>()

    @get:Input
    var buildArgs by project.objects.mapProperty<String, String?>()

    @get:OutputFile
    var imageIdFile by project.objects.property<File>()

    fun execute() {
        imageIdFile.writeText(
            client.buildImageCmd().apply {
                this@DockerBuild.buildArgs.forEach { (k, v) -> withBuildArg(k, v) }
                withTags(this@DockerBuild.tags.toSet())
                withBaseDirectory(contextFolder)
                withDockerfile(contextFolder.resolve("Dockerfile"))
            }.start().awaitCompletion().awaitImageId()
        )
    }
}
