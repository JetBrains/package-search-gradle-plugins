package org.jetbrains.gradle.plugins.docker

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.os.OperatingSystem
import java.io.File

abstract class DockerExtension(private val project: Project, private val name: String) : Named, ExtensionAware {

    /**
     * TCP or UNIX address to connect to the Docker client.
     * Defaults on Windows to `tcp://localhost:2376`, otherwise
     * `unix:///var/run/docker.sock`
     */
    var host: String =
        if (OperatingSystem.current().isWindows) "tcp://localhost:2376" else "unix:///var/run/docker.sock"

    /**
     * Enables TLS with the Docker client.
     */
    var useTsl = false

    /**
     * Specifies the path to the certificate to use for TSL.
     */
    var dockerCertPath: File? = null

    /**
     * Specifies the path to the certificate to use for TSL.
     */
    fun dockerCertFile(path: String) {
        dockerCertPath = project.file(path)
    }

    override fun getName() = name
}
