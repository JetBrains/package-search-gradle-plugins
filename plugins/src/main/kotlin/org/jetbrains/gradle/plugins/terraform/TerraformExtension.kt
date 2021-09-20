package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.jetbrains.gradle.plugins.propertyWithDefault
import org.jetbrains.gradle.plugins.reference
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply
import kotlin.properties.Delegates

abstract class TerraformExtension(
    private val name: String,
    private val sourceSets: NamedDomainObjectContainer<TerraformSourceSet>
) : Named, ExtensionAware {

    /**
     * Version of Terraform to use. Default is `1.0.6`.
     */
    var version = "1.0.6"

    var showPlanOutputInConsole = true
    var showInitOutputInConsole = true

    /**
     * Register the check to execute `terraform apply`. If `false`, `terraform apply` cannot be executed.
     * It is meant to avoid accidental executions while experimenting with the plugin.
     */
    @Deprecated(
        "Configure each source set separately using sourceSets extension.",
        ReplaceWith("sourceSets { all { executeApplyOnlyIf(action) } }")
    )
    fun executeApplyOnlyIf(action: Spec<TerraformApply>) {
        sourceSets.all { executeApplyOnlyIf(action) }
    }

    /**
     * Register the check to execute `terraform destroy`. If `false`, `terraform destroy` cannot be executed.
     * It is meant to avoid accidental executions while experimenting with the plugin.
     */
    @Deprecated(
        "Configure each source set separately using sourceSets extension.",
        ReplaceWith("sourceSets { all { executeDestroyOnlyIf(action) } }")
    )
    fun executeDestroyOnlyIf(action: Spec<TerraformApply>) {
        sourceSets.all { executeDestroyOnlyIf(action) }
    }

    override fun getName() =
        name
}
