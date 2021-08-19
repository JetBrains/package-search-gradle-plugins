package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.specs.Spec
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply
import java.io.File

abstract class TerraformExtension(project: Project, private val name: String) : Named, ExtensionAware {

    /**
     * Version of Terraform to use. Default is `1.0.3`.
     */
    var version = "1.0.3"

    var showPlanOutputInConsole = true
    var showInitOutputInConsole = true

    /**
     * Directory where lambdas will be stored. Can be used to pass artifacts to
     * `terraform apply` tasks:
     * ```kotlin
     * val myProject = project(":my-project-with-shadow-plugin-applied")
     * dependencies {
     *      lambdas(myProject)
     * }
     * terraform {
     *      sourceSets {
     *          main {
     *              planVariables = mapOf(
     *                  "myProjectFatJar" to "${lambdasDirectory.absolutePath}/${myProject.name}"
     *               )
     *          }
     *      }
     * }
     * ```
     */
    var lambdasDirectory: File = project.file("${project.buildDir}/terraform/lambdas")

    internal var applySpec = Spec<TerraformApply> {
        it.logger.error(
            """
            Please specify a criteria with which execute terraform apply:
            terraform {
                executeApplyOnlyIf { System.getenv("CAN_EXECUTE_TF_APPLY") == "true" }
            }
            Or disable the check:
            terraform {
                executeApplyOnlyIf { true }
            }
        """.trimIndent()
        )
        false
    }

    internal var destroySpec = Spec<TerraformApply> {
        it.logger.error(
            """
            Please specify a criteria with which execute terraform destroy:
            terraform {
                executeDestroyOnlyIf { System.getenv("CAN_EXECUTE_TF_DESTROY") == "true" }
            }
            Or disable the check:
            terraform {
                executeDestroyOnlyIf { true }
            }
        """.trimIndent()
        )
        false
    }

    /**
     * Register the check to execute `terraform apply`. If `false`, `terraform apply` cannot be executed.
     * It is meant to avoid accidental executions while experimenting with the plugin.
     */
    fun executeApplyOnlyIf(action: Spec<TerraformApply>) {
        applySpec = action
    }

    /**
     * Register the check to execute `terraform destroy`. If `false`, `terraform destroy` cannot be executed.
     * It is meant to avoid accidental executions while experimenting with the plugin.
     */
    fun executeDestroyOnlyIf(action: Spec<TerraformApply>) {
        destroySpec = action
    }

    override fun getName() =
        name
}
