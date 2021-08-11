package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.addAll
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.setValue
import java.io.File

open class DockerExecBuild : AbstractDockerExecTask(), DockerBuildSpec {

    init {
        logging.captureStandardError(LogLevel.INFO)
    }

    @get:Input
    @get:Optional
    override var tags by project.objects.listProperty<String>()

    @get:InputDirectory
    override var contextFolder by project.objects.property<File>()

    @get:Input
    override var buildArgs by project.objects.mapProperty<String, String?>()

    override fun provideArguments(): List<String> = buildList {
        add("build")
        tags.forEach { tag ->
            addAll("-t", tag)
        }
        buildArgs.forEach { (k, v) ->
            addAll("--build-arg", "$k=$v")
        }
        add(contextFolder.absolutePath)
    }
}
