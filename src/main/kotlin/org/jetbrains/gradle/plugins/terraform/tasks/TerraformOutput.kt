package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.*
import org.gradle.process.ExecSpec
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.propertyWithDefault
import org.jetbrains.gradle.plugins.setValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable

open class TerraformOutput : AbstractTerraformExec() {

    enum class Format : Serializable {
        JSON, RAW
    }

    @get:InputDirectory
    override var dataDir by project.objects.property<File>()

    @get:Input
    var format by project.objects.propertyWithDefault(Format.JSON)

    @get:Input
    var variables by project.objects.listProperty<String>()

    @get:OutputFile
    var outputFile by project.objects.property<File>()

    private val stdOutStream = ByteArrayOutputStream()

    init {
        @Suppress("LeakingThis")
        doLast { outputFile.writeText(stdOutStream.toByteArray().toString(Charsets.UTF_8)) }
    }

    override fun ExecSpec.customizeExec() {
        standardOutput = stdOutStream
    }

    override fun getTerraformArguments() = buildList {
        add("output")
        add("-no-color")
        when (format) {
            Format.JSON -> add("-json")
            Format.RAW -> add("-raw")
        }
        addAll(variables)
    }
}