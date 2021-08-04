package org.jetbrains.gradle.plugins.docker

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.os.OperatingSystem
import java.io.File

abstract class DockerExtension(private val project: Project, private val name: String) : Named, ExtensionAware {

    internal var remoteConfigBuilder: Remote? = null

    class Remote(private val project: Project) {

        /**
         * TCP or UNIX address to connect to the Docker client.
         * Defaults on Windows to `tcp://localhost:2376`, otherwise
         * `unix:///var/run/docker.sock`
         */
        var host: String =
            if (OperatingSystem.current().isWindows) "tcp://localhost:2375" else "unix:///var/run/docker.sock"

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
    }

    /**
     * Uses HTTP Docker API instead of the local command line.
     */
    fun useDockerRestApi(action: Remote.() -> Unit) {
        useDockerRestApi()
        remoteConfigBuilder?.apply(action)
    }

    /**
     * Uses HTTP Docker API instead of the local command line.
     */
    fun useDockerRestApi() {
        if (remoteConfigBuilder == null) {
            remoteConfigBuilder = Remote(project)
        }
    }

    override fun getName() = name
}
