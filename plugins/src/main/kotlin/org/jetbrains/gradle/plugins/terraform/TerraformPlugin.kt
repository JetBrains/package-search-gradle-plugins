package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.executeAllOn
import org.jetbrains.gradle.plugins.terraform.tasks.*
import org.jetbrains.gradle.plugins.toCamelCase
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

        val terraformExtension = extensions.create<TerraformExtension>(
            TERRAFORM_EXTENSION_NAME,
            project,
            TERRAFORM_EXTENSION_NAME
        )

        val sourceSets =
            container { name -> TerraformSourceSet(project, name.toCamelCase()) }

        terraformExtension.extensions.add("sourceSets", sourceSets)

        sourceSets.create("main")

        afterEvaluate {
            val copyLambdas by tasks.registering(Sync::class) {
                from(lambda)
                into(terraformExtension.lambdasDirectory)
            }
            tasks.create<TerraformExtract>(TERRAFORM_EXTRACT_TASK_NAME) {
                configuration = generateTerraformDetachedConfiguration(terraformExtension.version)
                val executableName = evaluateTerraformName(terraformExtension.version)
                outputExecutable = File(buildDir, "terraform/$executableName")
            }
            sourceSets.forEach { sourceSet: TerraformSourceSet ->
                val taskName = sourceSet.name.capitalize()

                val tfInit: TaskProvider<TerraformInit> =
                    tasks.register<TerraformInit>("terraform${taskName}Init") {
                        sourcesDirectory = sourceSet.srcDir
                        dataDir = sourceSet.dataDir
                        sourceSet.tasksProvider.initActions.executeAllOn(this)
                    }

                val terraformPlanOutputFile = file(
                    "$buildDir/terraform/" +
                            "${taskName.decapitalize()}/plan.bin"
                )

                val tfShow = tasks.create<TerraformShow>("terraform${taskName}Show") {
                    sourcesDirectory = sourceSet.srcDir
                    dataDir = sourceSet.dataDir
                    inputPlanFile = terraformPlanOutputFile
                    outputJsonPlanFile = terraformPlanOutputFile.resolveSibling("plan.json")
                    sourceSet.tasksProvider.showActions.executeAllOn(this)
                }

                val tfPlan = tasks.register<TerraformPlan>("terraform${taskName}Plan") {
                    dependsOn(tfInit, copyLambdas)
                    sourcesDirectory = sourceSet.srcDir
                    dataDir = sourceSet.dataDir
                    outputPlanFile = terraformPlanOutputFile
                    variables = sourceSet.planVariables
                    finalizedBy(tfShow)
                    sourceSet.tasksProvider.planActions.executeAllOn(this)
                }

                tfShow.dependsOn(tfPlan)

                tasks.register<TerraformApply>("terraform${taskName}Apply") {
                    dependsOn(tfPlan)
                    sourcesDirectory = sourceSet.srcDir
                    dataDir = sourceSet.dataDir
                    planFile = terraformPlanOutputFile
                    onlyIf {
                        val canExecuteApply = terraformExtension.applySpec.isSatisfiedBy(this)
                        if (!canExecuteApply) logger.warn(
                            "Cannot execute $name. Please check " +
                                    "your terraform extension in the script."
                        )
                        canExecuteApply
                    }
                    sourceSet.tasksProvider.applyActions.executeAllOn(this)
                }

                val terraformDestroyPlanOutputFile = file(
                    "$buildDir/terraform/" +
                            "${taskName.decapitalize()}/destroyPlan.bin"
                )

                val tfDestroyShow = tasks.create<TerraformShow>("terraform${taskName}DestroyShow") {
                    inputPlanFile = terraformDestroyPlanOutputFile
                    dataDir = sourceSet.dataDir
                    sourcesDirectory = sourceSet.srcDir
                    outputJsonPlanFile = terraformDestroyPlanOutputFile.resolveSibling("destroyPlan.json")
                    sourceSet.tasksProvider.destroyShowActions.executeAllOn(this)
                }

                val tfDestroyPlan = tasks.register<TerraformPlan>("terraform${taskName}DestroyPlan") {
                    dependsOn(tfInit, copyLambdas)
                    sourcesDirectory = sourceSet.srcDir
                    dataDir = sourceSet.dataDir
                    outputPlanFile = terraformDestroyPlanOutputFile
                    isDestroy = true
                    variables = sourceSet.planVariables
                    finalizedBy(tfDestroyShow)
                    sourceSet.tasksProvider.destroyPlanActions.executeAllOn(this)
                }

                tfDestroyShow.dependsOn(tfDestroyPlan)

                tasks.register<TerraformApply>("terraform${taskName}Destroy") {
                    dependsOn(tfDestroyPlan)
                    sourcesDirectory = sourceSet.srcDir
                    dataDir = sourceSet.dataDir
                    planFile = terraformDestroyPlanOutputFile
                    onlyIf {
                        val canExecuteApply = terraformExtension.destroySpec.isSatisfiedBy(this)
                        if (!canExecuteApply) logger.warn(
                            "Cannot execute $name. Please check " +
                                    "your terraform extension in the script."
                        )
                        canExecuteApply
                    }
                    sourceSet.tasksProvider.destroyActions.executeAllOn(this)
                }
            }
        }
    }

}
