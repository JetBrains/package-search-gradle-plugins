package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.specs.Spec
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply

abstract class TerraformExtension(
    private val name: String,
) : Named, ExtensionAware {

    internal val mavenPublicationActions = mutableListOf<Action<MavenPublication>>()

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
    fun executeApplyOnlyIf(action: Spec<TerraformApply>) =
        extensions.getByName<NamedDomainObjectContainer<TerraformSourceSet>>("sourceSets")
            .all { executeApplyOnlyIf(action) }

    /**
     * Register the check to execute `terraform destroy`. If `false`, `terraform destroy` cannot be executed.
     * It is meant to avoid accidental executions while experimenting with the plugin.
     */
    @Deprecated(
        "Configure each source set separately using sourceSets extension.",
        ReplaceWith("sourceSets { all { executeDestroyOnlyIf(action) } }")
    )
    fun executeDestroyOnlyIf(action: Spec<TerraformApply>) =
        extensions.getByName<NamedDomainObjectContainer<TerraformSourceSet>>("sourceSets")
            .all { executeDestroyOnlyIf(action) }

    fun mavenPublication(action: Action<MavenPublication>) {
        mavenPublicationActions.add(action)
    }

    override fun getName() =
        name
}
