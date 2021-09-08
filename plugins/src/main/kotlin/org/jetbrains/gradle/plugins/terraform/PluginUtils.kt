package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.executeAllOn
import org.jetbrains.gradle.plugins.terraform.tasks.AbstractTerraformExec
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply
import org.jetbrains.gradle.plugins.toCamelCase
import java.io.File

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

internal fun Project.registerLifecycleTasks(): List<TaskProvider<Task>> {
    val terraformInit by tasks.registering {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformShow by tasks.registering {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformDestroyShow by tasks.registering {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformPlan by tasks.registering {
        group = TerraformPlugin.TASK_GROUP
        dependsOn(terraformInit)
        finalizedBy(terraformShow)
    }
    terraformShow { dependsOn(terraformPlan) }
    val terraformDestroyPlan by tasks.registering {
        group = TerraformPlugin.TASK_GROUP
        dependsOn(terraformInit)
        finalizedBy(terraformDestroyShow)
    }
    val terraformApply by tasks.registering {
        group = TerraformPlugin.TASK_GROUP
    }
    val terraformDestroy by tasks.registering {
        group = TerraformPlugin.TASK_GROUP
    }
    return listOf(
        terraformInit, terraformShow, terraformDestroyShow,
        terraformPlan, terraformDestroyPlan, terraformApply, terraformDestroy
    )
}

internal fun Project.createExtension(): Pair<TerraformExtension, NamedDomainObjectContainer<TerraformSourceSet>> {
    val terraformExtension = extensions.create<TerraformExtension>(
        TerraformPlugin.TERRAFORM_EXTENSION_NAME,
        TerraformPlugin.TERRAFORM_EXTENSION_NAME
    )

    val sourceSets =
        container { name -> TerraformSourceSet(project, name.toCamelCase()) }

    terraformExtension.extensions.add("sourceSets", sourceSets)

    sourceSets.create("main")

    return terraformExtension to sourceSets
}

internal inline fun <reified T : AbstractTerraformExec> TaskContainer.terraformRegister(
    name: String,
    sourceSet: TerraformSourceSet,
    crossinline action: T.() -> Unit
) = register<T>(name) {
    sourcesDirectory = sourceSet.runtimeExecutionDirectory
    dataDir = sourceSet.dataDir
    action()
}

internal fun Project.createComponent(
    taskName: String,
    terraformApi: Configuration,
    terraformModuleZip: Zip,
    sourceSet: TerraformSourceSet,
    softwareComponentFactory: SoftwareComponentFactory
) {
    val terraformOutgoingElements =
        configurations.create("terraform${taskName}OutgoingElements") {
            isCanBeConsumed = true
            extendsFrom(terraformApi)
            outgoing.artifact(terraformModuleZip)
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(TerraformPlugin.Attributes.USAGE))
                attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(TerraformPlugin.Attributes.LIBRARY_ELEMENTS)
                )
            }
        }

    val component = softwareComponentFactory.adhoc(buildString {
        append("terraform")
        if (sourceSet.name != "main") append(taskName)
    })

    components.add(component)

    component.addVariantsFromConfiguration(terraformOutgoingElements) {
        mapToMavenScope("runtime")
    }

    plugins.withId("org.gradle.maven-publish") {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("terraform$taskName") {
                    groupId = project.group.toString()
                    artifactId = buildString {
                        append(project.name)
                        if (sourceSet.name != "main") append("-${sourceSet.name}")
                    }
                    version = project.version.toString()

                    from(component)
                }
            }
        }
    }
}

internal fun TaskContainer.terraformRegisterApply(
    name: String,
    sourceSet: TerraformSourceSet,
    dependsOn: TaskProvider<out Task>,
    spec: Spec<TerraformApply>,
    binaryPlanFile: File,
    configurations: List<Action<TerraformApply>>
) = terraformRegister<TerraformApply>(name, sourceSet) {
    dependsOn(dependsOn)
    planFile = binaryPlanFile
    onlyIf {
        val canExecuteApply = spec.isSatisfiedBy(this)
        if (!canExecuteApply) logger.warn(
            "Cannot execute $name. Please check " +
                    "your terraform extension in the script."
        )
        canExecuteApply
    }
    configurations.executeAllOn(this)
}