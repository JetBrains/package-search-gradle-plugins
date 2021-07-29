package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.specs.Spec
import org.gradle.kotlin.dsl.container
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply
import javax.inject.Inject

open class TerraformExtension(project: Project, private val name: String) : Named {

    var version = "1.0.3"

    val sourceSets: TerraformDirectorySetContainer = project.container { name -> TerraformDirectorySet(project, name) }

    /**
     * Directory where lambdas will be stored. Can be used to pass artifacts to surces.
     */
    var lambdasDirectory = project.file("${project.buildDir}/terraform/lambdas")

    internal var applySpec = Spec<TerraformApply> {
        it.logger.error("""
            Please specify a criteria with which execute terraform apply:
            terraform {
                executeApplyOnlyIf { System.getenv("CAN_EXECUTE_TF_APPLY") == "true" }
            }
            Or disable the check:
            terraform {
                executeApplyOnlyIf { true }
            }
        """.trimIndent())
        false
    }

    fun executeApplyOnlyIf(action: Spec<TerraformApply>) {
        applySpec = action
    }

    override fun getName() =
        name
}
