package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.jetbrains.gradle.plugins.propertyWithDefault

abstract class TerraformInit : AbstractTerraformExec() {

    @get:Input
    var useBackend by project.objects.propertyWithDefault(true)

    override fun getTerraformArguments() =
        listOf("init", "-input=false", "-backend:$useBackend")

}
