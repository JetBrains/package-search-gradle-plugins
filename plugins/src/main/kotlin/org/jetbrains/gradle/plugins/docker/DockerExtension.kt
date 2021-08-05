package org.jetbrains.gradle.plugins.docker

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.os.OperatingSystem
import java.io.File

abstract class DockerExtension(
    private val file: (String) -> File,
    val registries: DockerRegistryContainer,
    private val name: String
) : Named, ExtensionAware {

    internal var remoteConfigBuilder: Remote? = null

    class Remote(private val file: (String) -> File) {

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
            dockerCertPath = file(path)
        }
    }

    fun registries(action: Action<DockerRegistryContainer>) {
        action.execute(registries)
    }

    /**
     * Uses HTTP Docker API instead of the local command line. Default settings uses
     * the Docker unix port or `localhost:2375` on Windows with TLS disabled.
     */
    fun useDockerRestApi(action: Remote.() -> Unit) {
        useDockerRestApi()
        remoteConfigBuilder?.apply(action)
    }

    /**
     * Uses HTTP Docker API instead of the local command line. Default settings uses
     * the Docker unix port or `localhost:2375` on Windows with TLS disabled.
     */
    fun useDockerRestApi() {
        if (remoteConfigBuilder == null) {
            remoteConfigBuilder = Remote(file)
        }
    }

    override fun getName() = name
}
