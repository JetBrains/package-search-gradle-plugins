package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Named
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.specs.Spec
import org.jetbrains.gradle.plugins.reference
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply
import kotlin.properties.Delegates

abstract class TerraformExtension(private val name: String) : Named, ExtensionAware {

    /**
     * Version of Terraform to use. Default is `1.0.6`.
     */
    var version = "1.0.6"

    var showPlanOutputInConsole = true
    var showInitOutputInConsole = true

    internal var applySpec by Delegates.reference {
        Spec<TerraformApply> {
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
    }

    internal var destroySpec by Delegates.reference {
        Spec<TerraformApply> {
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
