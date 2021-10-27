package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.Task
import java.io.File

interface DockerBuildSpec : Task {
    var tags: List<String>
    var contextFolder: File
    var buildArgs: Map<String, String?>
}
