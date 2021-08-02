package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.specs.Spec
import org.gradle.kotlin.dsl.NamedDomainObjectContainerScope
import org.gradle.kotlin.dsl.container
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply

open class TerraformExtension(project: Project, private val name: String) : Named {

    /**
     * Version of Terraform to use. Default is `1.0.3`.
     */
    var version = "1.0.3"

    /**
     * Container for Terraform sources.
     */
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

    /**
     * Register the check to execute `terraform apply`. If returns `false` `terraform apply` cannot be executed.
     * It is meant to avoid accidental executions while experimenting with the plugin.
     */
    fun executeApplyOnlyIf(action: Spec<TerraformApply>) {
        applySpec = action
    }

    fun sourceSets(action: Action<NamedDomainObjectContainerScope<TerraformDirectorySet>>) {
        sourceSets.invoke(action)
    }

    override fun getName() =
        name
}
