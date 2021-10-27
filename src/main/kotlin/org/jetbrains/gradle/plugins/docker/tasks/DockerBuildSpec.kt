package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import java.io.File

interface DockerBuildSpec : Task {
    @get:Input
    var tags: List<String>

    @get:InputDirectory
    var contextFolder: File

    @get:Input
    var buildArgs: Map<String, String?>
}
