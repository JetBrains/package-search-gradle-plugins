package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.gradle.process.ExecSpec
import java.io.File

abstract class TerraformShow : AbstractTerraformExec() {

    @get:InputFile
    var inputPlanFile by project.objects.property<File>()

    @get:OutputFile
    var outputJsonPlanFile by project.objects.property<File>()

    override fun ExecSpec.customizeExec() {
        standardOutput = outputJsonPlanFile.outputStream()
    }

    override fun getTerraformArguments(): List<String> =
        listOf("show", "-json", inputPlanFile.absolutePath)

}
