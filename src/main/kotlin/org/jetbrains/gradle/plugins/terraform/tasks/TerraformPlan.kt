package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.*
import java.io.File

open class TerraformPlan : AbstractTerraformExec() {

    @get:Input
    var isDestroy by project.objects.propertyWithDefault(false)

    @get:Input
    var variables by project.objects.mapProperty<String, String?>()

    @get:Input
    var fileVariables by project.objects.mapProperty<String, File>()

    @get:InputFile
    @get:Optional
    var variablesFile by project.objects.nullableProperty<File>()

    @get:Input
    @get:Optional
    var refresh: Boolean? by project.objects.nullableProperty()

    @get:Input
    @get:Optional
    var replace: String? by project.objects.nullableProperty()

    @get:Input
    @get:Optional
    var target by project.objects.nullableProperty<String>()

    @get:Input
    var parallelism by project.objects.propertyWithDefault(10)

    @get:OutputFile
    var outputPlanFile by project.objects.property<File>()

    @get:InputDirectory
    override var dataDir by project.objects.property<File>()

    override fun getTerraformArguments(): List<String> = buildList {
        add("plan")
        add("-input=false")
        for ((k, v) in variables + fileVariables.mapValues { it.value.readText() }) {
            addAll("-var", "$k=$v")
        }
        variablesFile?.run { add("-var-file=$absolutePath") }
        refresh?.let { add("-refresh=$it") }
        replace?.let { add("-replace=$it") }
        target?.let { add("-replace=$it") }
        if (isDestroy) add("-destroy")
        addAll("-parallelism=$parallelism")
        addAll("-out", outputPlanFile.absolutePath)
    }
}
