package org.jetbrains.gradle.plugins.terraform.tasks

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.jetbrains.gradle.plugins.terraform.TerraformModuleMetadata
import java.io.File

open class GenerateTerraformMetadata : DefaultTask() {

    companion object {
        private val json by lazy {
            Json {
                prettyPrint = true
            }
        }
    }

    @get:Input
    var metadata by project.objects.property<TerraformModuleMetadata>()

    @get:OutputFile
    var outputFile by project.objects.property<File>()

    @TaskAction
    fun writeJson() {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(json.encodeToString(metadata))
    }
}