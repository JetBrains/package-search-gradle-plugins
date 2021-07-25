package org.jetbrains.gradle.plugins.terraform.tasks

import org.jetbrains.gradle.plugins.addAll
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecSpec
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.setValue
import java.io.File

abstract class TerraformApply : AbstractTerraformExec() {

    @get:Input
    var variables by project.objects.mapProperty<String, String>()

    @get:InputFile
    @get:Optional
    val variablesFile = project.objects.property<File>()

    override fun ExecSpec.customizeExec() {
        args = buildList {
            add("apply")
            add("-auto-approve")
            for ((k, v) in variables) {
                addAll("-var", "$k=$v")
            }
            if (variablesFile.isPresent) add("-var-file=${variablesFile.get().absolutePath}")
        }
    }
}
