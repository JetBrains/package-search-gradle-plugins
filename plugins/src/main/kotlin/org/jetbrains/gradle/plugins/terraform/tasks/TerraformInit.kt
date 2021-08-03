package org.jetbrains.gradle.plugins.terraform.tasks

abstract class TerraformInit : AbstractTerraformExec() {

    override fun getTerraformArguments() =
        listOf("init", "-input=false")

}
