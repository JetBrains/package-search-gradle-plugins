package org.jetbrains.gradle.plugins.terraform.tasks

import org.jetbrains.gradle.plugins.addAll
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setValue
import org.gradle.process.ExecSpec
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.propertyWithDefault
import org.jetbrains.gradle.plugins.setValue
import java.io.File

abstract class TerraformPlan : AbstractTerraformExec() {

    @get:Input
    var isDestroy by project.objects.propertyWithDefault(false)

    @get:Input
    var variables by project.objects.mapProperty<String, String>()

    @get:InputFile
    @get:Optional
    val variablesFile = project.objects.property<File>()

    override fun ExecSpec.customizeExec() {
        args = buildList {
            add("plan")
            for ((k, v) in variables) {
                addAll("-var", "$k=$v")
            }
            if (variablesFile.isPresent) add("-var-file=${variablesFile.get().absolutePath}")
            if (isDestroy) add("-destroy")
        }
    }
}
