package org.jetbrains.gradle.plugins.docker

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.container
import org.jetbrains.gradle.plugins.DockerImagesContainer
import org.jetbrains.gradle.plugins.DockerRepositoriesContainer
import java.io.File

open class DockerExtension(private val project: Project, private val name: String) : Named {

    var host: String = if (OperatingSystem.current().isWindows) "tcp://localhost:2376" else "unix:///var/run/docker.sock"
    var useTsl = false
    var dockerCertPath: File? = null

    fun dockerCertFile(path: String) {
        dockerCertPath = project.file(path)
    }

    val repositories: DockerRepositoriesContainer = project.container { name ->
        DockerRepository(name)
    }

    val images: DockerImagesContainer =  project.container { name ->
            DockerImage(
                name,
                project.version as? String ?: "latest",
                name,
                project
            )
        }

    override fun getName() = name
}
