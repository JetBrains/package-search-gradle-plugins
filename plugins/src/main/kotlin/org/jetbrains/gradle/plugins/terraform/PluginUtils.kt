package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.maybeCreating
import org.jetbrains.gradle.plugins.toCamelCase

internal fun Project.setupTerraformRepository() {
    repositories {
        ivy {
            name = "Terraform Executable Repository"
            url = uri("https://releases.hashicorp.com/terraform/")
            patternLayout {
                artifact("[revision]/[artifact]_[revision]_[classifier].zip")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("hashicorp", "terraform")
            }
        }
    }
}

internal fun Project.registerLifecycleTasks(): List<Task> {
    val terraformInit by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformShow by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformDestroyShow by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformPlan by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
        dependsOn(terraformInit)
        finalizedBy(terraformShow)
    }
    terraformShow.dependsOn(terraformPlan)
    val terraformDestroyPlan by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
        dependsOn(terraformInit)
        finalizedBy(terraformDestroyShow)
    }
    val terraformApply by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformDestroy by tasks.creating {
        group = TerraformPlugin.TASK_GROUP
    }
    return listOf(
        terraformInit, terraformShow, terraformDestroyShow,
        terraformPlan, terraformDestroyPlan, terraformApply, terraformDestroy
    )
}

internal fun Project.createConfigurations(): List<Configuration> {
    val lambda: Configuration by configurations.creating {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
        }
    }
    val terraformApi by configurations.maybeCreating {
        isCanBeConsumed = true
    }
    val terraformImplementation by configurations.maybeCreating {
        isCanBeConsumed = true
        extendsFrom(terraformApi)
    }
    return listOf(lambda, terraformImplementation, terraformApi)
}

internal fun Project.createExtension(): Pair<TerraformExtension, NamedDomainObjectContainer<TerraformSourceSet>> {
    val terraformExtension = extensions.create<TerraformExtension>(
        TerraformPlugin.TERRAFORM_EXTENSION_NAME,
        project,
        TerraformPlugin.TERRAFORM_EXTENSION_NAME
    )

    val sourceSets =
        container { name -> TerraformSourceSet(project, name.toCamelCase()) }

    terraformExtension.extensions.add("sourceSets", sourceSets)

    sourceSets.create("main")
    return terraformExtension to sourceSets
}
