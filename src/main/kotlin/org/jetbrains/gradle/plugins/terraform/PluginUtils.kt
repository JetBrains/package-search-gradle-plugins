package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.executeAllOn
import org.jetbrains.gradle.plugins.maybeRegister
import org.jetbrains.gradle.plugins.terraform.tasks.AbstractTerraformExec
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformExtract
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

    val sourceSets =
        container { name -> TerraformSourceSet(project, name.toCamelCase()) }

    val terraformExtension = extensions.create<TerraformExtension>(
        TerraformPlugin.TERRAFORM_EXTENSION_NAME,
        TerraformPlugin.TERRAFORM_EXTENSION_NAME,
        sourceSets
    )

    terraformExtension.extensions.add("sourceSets", sourceSets)

    sourceSets.create("main").apply { addLambdasToResources = true }

    return terraformExtension to sourceSets
}

internal fun AbstractTerraformExec.attachSourceSet(sourceSet: TerraformSourceSet) {
    sourcesDirectory = sourceSet.runtimeExecutionDirectory
    dataDir = sourceSet.dataDir
}

internal fun Project.createDistribution(
    sourceSet: TerraformSourceSet,
    copyExecutionContext: TaskProvider<Sync>,
    terraformExtract: TaskProvider<TerraformExtract>
) {
    configure<DistributionContainer> {
        maybeRegister(sourceSet.name) {
            contents {
                from(copyExecutionContext)
                from(terraformExtract) {
                    rename {
                        buildString {
                            append("terraform")
                            if (OperatingSystem.current().isWindows)
                                append(".exe")
                        }
                    }
                }
            }
        }
    }
}

internal fun Project.createComponent(
    terraformApi: Configuration,
    terraformModuleZip: TaskProvider<Zip>,
    softwareComponentFactory: SoftwareComponentFactory,
    terraformExtension: TerraformExtension
) {
    val terraformOutgoingElements =
        configurations.create("terraformOutgoingElements") {
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

    val component = softwareComponentFactory.adhoc("terraform")

    components.add(component)

    component.addVariantsFromConfiguration(terraformOutgoingElements) {
        mapToMavenScope("runtime")
    }

    plugins.withId("org.gradle.maven-publish") {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("terraform") {
                    groupId = project.group.toString()
                    artifactId = project.name
                    version = project.version.toString()
                    from(component)
                    afterEvaluate { terraformExtension.mavenPublicationActions.executeAllOn(this@create) }
                }
            }
        }
    }
}

internal fun TerraformApply.configureApply(
    dependsOn: TaskProvider<out Task>,
    spec: Property<Spec<TerraformApply>>,
    binaryPlanFile: File,
    configurations: List<Action<TerraformApply>>
) {
    dependsOn(dependsOn)
    planFile = binaryPlanFile
    onlyIf {
        val canExecuteApply = spec.get().isSatisfiedBy(this)
        if (!canExecuteApply) logger.warn(
            "Cannot execute $name. Please check " +
                    "your terraform extension in the script."
        )
        canExecuteApply
    }
    configurations.executeAllOn(this)
}

internal fun <T : Task> Collection<TaskProvider<T>>.configureEach(action: T.() -> Unit) =
    forEach { it { action() } }