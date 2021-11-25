package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.jetbrains.gradle.plugins.addAll
import org.jetbrains.gradle.plugins.getValue
import org.jetbrains.gradle.plugins.nullableProperty
import org.jetbrains.gradle.plugins.propertyWithDefault
import org.jetbrains.gradle.plugins.setValue
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
        for ((k, v) in variables + fileVariables.mapValues { it.value.takeIf { it.exists() }?.readText() }) {
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
