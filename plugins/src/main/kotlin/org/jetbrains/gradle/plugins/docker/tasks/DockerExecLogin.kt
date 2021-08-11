package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.jetbrains.gradle.plugins.nullableProperty

open class DockerExecLogin : AbstractDockerExecTask() {

    @get:Input
    var username: String? by project.objects.nullableProperty()

    @get:Input
    var password: String? by project.objects.nullableProperty()

    @get:Input
    var url: String by project.objects.property()

    override fun provideArguments() =
        listOf(
            "login",
            "-u",
            requireNotNull(username) { "Username for $url is null" },
            "-p",
            requireNotNull(password) { "Password for $url is null" },
            url
        )
}
