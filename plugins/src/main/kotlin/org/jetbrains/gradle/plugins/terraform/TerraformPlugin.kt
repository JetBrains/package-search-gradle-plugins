package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Bundling
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.component6
import org.jetbrains.gradle.plugins.component7
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformExtract
import java.io.File
import javax.inject.Inject

open class TerraformPlugin @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory
) : Plugin<Project> {

    object Attributes {

        const val USAGE = "terraform"
        const val LIBRARY_ELEMENTS = "zip-archive"

        val SOURCE_SET_NAME_ATTRIBUTE: Attribute<String> = Attribute.of("terraform.sourceset.name", String::class.java)
    }

    companion object {

        const val TERRAFORM_EXTRACT_TASK_NAME = "terraformExtract"
        const val TERRAFORM_EXTENSION_NAME = "terraform"
        const val TASK_GROUP = "terraform"

    }

    override fun apply(target: Project): Unit = with(target) {

        setupTerraformRepository()

        val lambda: Configuration by configurations.creating {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
            }
        }
        val terraformApi by configurations.creating {
            isCanBeConsumed = true
        }
        val terraformImplementation by configurations.creating {
            isCanBeConsumed = true
            extendsFrom(terraformApi)
        }

        val (terraformExtension, sourceSets) = createExtension()

        val (terraformInit, terraformShow, terraformDestroyShow,
            terraformPlan, terraformDestroyPlan, terraformApply,
            terraformDestroy) = registerLifecycleTasks()

        val terraformExtract = tasks.register<TerraformExtract>(TERRAFORM_EXTRACT_TASK_NAME)

        sourceSets.all {
            val tasksToConfigure = TFTaskContainer(
                project = project,
                sourceSet = this,
                terraformApi = terraformApi,
                terraformImplementation = terraformImplementation,
                lambdaConfiguration = lambda,
                terraformExtract = terraformExtract,
                terraformExtension = terraformExtension,
                terraformInit = terraformInit,
                terraformShow = terraformShow,
                terraformPlan = terraformPlan,
                terraformApply = terraformApply,
                terraformDestroyShow = terraformDestroyShow,
                terraformDestroyPlan = terraformDestroyPlan,
                terraformDestroy = terraformDestroy,
                softwareComponentFactory = softwareComponentFactory
            )
            afterEvaluate { tasksToConfigure.configureTasksForSourceSet() }
        }

        afterEvaluate {

            terraformExtract {
                configuration = generateTerraformDetachedConfiguration(terraformExtension.version)
                val executableName = evaluateTerraformName(terraformExtension.version)
                outputExecutable = File(buildDir, "terraform/$executableName")
            }

        }
    }
}