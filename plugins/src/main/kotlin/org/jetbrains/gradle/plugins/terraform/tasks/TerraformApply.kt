package org.jetbrains.gradle.plugins.terraform.tasks

import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import java.io.File

open class TerraformApply : AbstractTerraformExec() {

    @get:InputFile
    var planFile by project.objects.property<File>()

    @get:InputDirectory
    override var dataDir by project.objects.property<File>()

    override fun getTerraformArguments(): List<String> = listOf("apply", planFile.absolutePath)

}
