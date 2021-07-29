package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.executeAllOn
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformApply
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformExtract
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformInit
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformPlan
import java.io.File

open class TerraformPlugin : Plugin<Project> {

    companion object {

        const val TERRAFORM_EXTRACT_TASK_NAME = "terraformExtract"
        const val TERRAFORM_EXTENSION_NAME = "terraform"
        const val TASK_GROUP = "terraform"
    }

    override fun apply(target: Project): Unit = with(target) {
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

        val lambda: Configuration by configurations.creating {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling.SHADOWED))
            }
        }

        val ext = extensions.create<TerraformExtension>(
            TERRAFORM_EXTENSION_NAME,
            project,
            TERRAFORM_EXTENSION_NAME
        )

        ext.sourceSets.create("main")

        afterEvaluate {
            val copyLambdas by tasks.registering(Sync::class) {
                from(lambda)
                into(ext.lambdasDirectory)
            }
            tasks.create<TerraformExtract>(TERRAFORM_EXTRACT_TASK_NAME) {
                configuration = generateTerraformDetachedConfiguration(ext.version)
                val executableName = evaluateTerraformName(ext.version)
                outputExecutable = File(buildDir, "terraform/$executableName")
            }
            ext.sourceSets.forEach { sourceSet: TerraformDirectorySet ->
                val taskName = sourceSet.name.capitalize()
                val terraformInit: TaskProvider<TerraformInit> =
                    tasks.register<TerraformInit>("terraform${taskName}Init") {
                        sourcesDirectory = sourceSet.srcDir
                        sourceSet.tasksProvider.initActions.executeAllOn(this)
                    }
                tasks.register<TerraformPlan>("terraform${taskName}Plan") {
                    dependsOn(terraformInit, copyLambdas)
                    sourcesDirectory = sourceSet.srcDir
                    sourceSet.tasksProvider.planActions.executeAllOn(this)
                }
                tasks.register<TerraformApply>("terraform${taskName}Apply") {
                    dependsOn(terraformInit, copyLambdas)
                    sourcesDirectory = sourceSet.srcDir
                    onlyIf {
                        val canExecuteApply = ext.applySpec.isSatisfiedBy(this)
                        if (!canExecuteApply) logger.warn(
                            "Cannot execute $name. Please check " +
                                    "your terraform extension in the script."
                        )
                        canExecuteApply
                    }
                    sourceSet.tasksProvider.applyActions.executeAllOn(this)
                }
            }
        }
    }

}
