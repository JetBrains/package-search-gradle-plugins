package org.jetbrains.gradle.plugins.docker

import org.gradle.api.Named

open class DockerRepository(
    private val name: String,
    var username: String = "",
    var password: String = "",
    var email: String = "",
    var url: String = "",
    var imageNamePrefix: String = url
) : Named {
    override fun getName() = name
}
