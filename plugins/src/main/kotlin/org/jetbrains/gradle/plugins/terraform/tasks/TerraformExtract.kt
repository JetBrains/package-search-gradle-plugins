package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setValue
import java.io.File

open class TerraformExtract : DefaultTask() {

    @get:InputFiles
    var configuration by project.objects.property<Configuration>()

    @get:OutputFile
    var outputExecutable by project.objects.property<File>()

    @TaskAction
    fun extract() {
        project.zipTree(configuration.resolve().single()).first {
            it.nameWithoutExtension == "terraform"
                    && if (OperatingSystem.current().isWindows) it.extension == "exe" else true
        }.copyTo(outputExecutable, true)
        if (OperatingSystem.current().isUnix) outputExecutable.setExecutable(true)
    }
}
