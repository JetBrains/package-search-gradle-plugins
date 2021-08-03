package org.jetbrains.gradle.plugins.docker

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.os.OperatingSystem
import java.io.File

abstract class DockerExtension(private val project: Project, private val name: String) : Named, ExtensionAware {

    var host: String =
        if (OperatingSystem.current().isWindows) "tcp://localhost:2376" else "unix:///var/run/docker.sock"
    var useTsl = false
    var dockerCertPath: File? = null

    fun dockerCertFile(path: String) {
        dockerCertPath = project.file(path)
    }

    override fun getName() = name
}
