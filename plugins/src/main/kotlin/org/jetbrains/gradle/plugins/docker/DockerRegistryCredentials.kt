package org.jetbrains.gradle.plugins.docker

import org.gradle.api.Named

/**
 * Data model for a Docker Repository (or registry). This class store all the
 * information necessary to publish a Docker Image.
 *
 * @param username Username used to log in the repository, optional if [email] is provided.
 * @param password Password used to log in the repository.
 * @param email Email used to log in the repository, optional if [username] is provided.
 * @param url Url of the repository to log in.
 * @param imageNamePrefix The prefix that is needed to be added to an image tag to correctly push
 * it in the repository.
 */
data class DockerRegistryCredentials(
    private val name: String,
    var username: String? = "",
    var password: String? = "",
    var email: String? = "",
    var url: String = "",
    var imageNamePrefix: String = url
) : Named {
    override fun getName() = name
}


/**
 * Data model for a Docker Hub. This class store all the
 * information necessary to publish a Docker Image on the Hub.
 *
 * @param username Username used to log in the repository.
 * @param password Password used to log in the repository.
 */
data class DockerHubCredentials(
    var username: String? = "",
    var password: String? = ""
)
