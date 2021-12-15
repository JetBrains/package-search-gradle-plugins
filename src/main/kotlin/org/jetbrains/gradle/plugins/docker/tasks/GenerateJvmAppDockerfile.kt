package org.jetbrains.gradle.plugins.docker.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.jetbrains.gradle.plugins.docker.JvmBaseImages
import org.jetbrains.gradle.plugins.property

open class GenerateJvmAppDockerfile : AbstractGenerateDockerfile() {

    companion object {
        private val template = """
        FROM %%%JVM_IMAGE_NAME%%%

        COPY bin /%%%APP_NAME%%%/bin
        COPY lib /%%%APP_NAME%%%/lib

        CMD ["/%%%APP_NAME%%%/bin/%%%APP_NAME%%%"]
    """.trimIndent()
    }

    @get:Input
    var baseImage: JvmBaseImages by project.objects.property(JvmBaseImages.OpenJRE8Slim)

    @get:Input
    var appName by project.objects.property<String>()

    @TaskAction
    fun execute() {
        outputDockerfile.parentFile.mkdirs()
        outputDockerfile.writeText(
            template.replace("%%%JVM_IMAGE_NAME%%%", baseImage.toString())
                .replace("%%%APP_NAME%%%", appName)
        )
    }
}
