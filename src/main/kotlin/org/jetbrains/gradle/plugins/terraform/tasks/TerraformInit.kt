package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import org.jetbrains.gradle.plugins.addAll
import org.jetbrains.gradle.plugins.property
import java.io.File

open class TerraformInit : AbstractTerraformExec() {

    @get:Input
    var useBackend by project.objects.property(true)

    @get:OutputDirectory
    override var dataDir by project.objects.property<File>()

    override fun getTerraformArguments() = buildList<String> {
        addAll("init", "-input=false")
        if (!useBackend) add("-backend=true")
    }

}
