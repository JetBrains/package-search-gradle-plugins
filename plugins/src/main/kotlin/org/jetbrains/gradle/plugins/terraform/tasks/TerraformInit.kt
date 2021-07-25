package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.setValue
import org.gradle.process.ExecSpec
import org.jetbrains.gradle.plugins.propertyWithDefault

abstract class TerraformInit : AbstractTerraformExec() {

    @get:Input
    var askForInput by project.objects.propertyWithDefault(false)

    override fun ExecSpec.customizeExec() {
        args = buildList {
            add("init")
            // check if there is a console to even input something!
            val r = if (System.console() != null) askForInput else false
            add("-input=$r")
        }
    }
}
