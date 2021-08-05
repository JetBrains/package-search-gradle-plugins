package org.jetbrains.gradle.plugins.docker

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

class DockerRegistryContainer(
    private val container: NamedDomainObjectContainer<DockerRegistryCredentials>
) : NamedDomainObjectContainer<DockerRegistryCredentials> by container {

    /**
     * Adds the credentials for the Docker Hub.
     */
    fun dockerHub(action: Action<DockerHubCredentials>) =
        maybeCreate("dockerHub").apply {
            val hubCredentials = DockerHubCredentials().applyAction(action)
            username = hubCredentials.username
            password = hubCredentials.password
            url = "registry.hub.docker.com"
            imageNamePrefix = "$url/$username"
        }
}
