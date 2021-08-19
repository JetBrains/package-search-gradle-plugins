package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.tasks.Sync
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
        const val LIBRARY_ELEMENTS = "ZIP_ARCHIVE"

        val SOURCE_SET_NAME_ATTRIBUTE: Attribute<String> = Attribute.of("terraform.sourceset.name", String::class.java)
    }

    companion object {

        const val TERRAFORM_EXTRACT_TASK_NAME = "terraformExtract"
        const val TERRAFORM_EXTENSION_NAME = "terraform"
        const val TASK_GROUP = "terraform"
    }

    override fun apply(target: Project): Unit = with(target) {

        setupTerraformRepository()

        val (lambda, terraformImplementation, terraformApi) = createConfigurations()

        val (terraformExtension, sourceSets, main) = createExtension()

        val (terraformInit, terraformShow, terraformDestroyShow,
            terraformPlan, terraformDestroyPlan, terraformApply,
            terraformDestroy) = registerLifecycleTasks()

        main.resourcesDirs.add(terraformExtension.lambdasDirectory)

        val copyLambdas by tasks.registering(Sync::class)

        val terraformExtract = tasks.register<TerraformExtract>(TERRAFORM_EXTRACT_TASK_NAME)

        elaborateSourceSet(
            main,
            terraformApi,
            terraformImplementation,
            copyLambdas,
            terraformExtract,
            terraformExtension,
            terraformInit,
            terraformShow,
            terraformPlan,
            terraformApply,
            terraformDestroyShow,
            terraformDestroyPlan,
            terraformDestroy,
            softwareComponentFactory
        )

        afterEvaluate {

            terraformExtract {
                configuration = generateTerraformDetachedConfiguration(terraformExtension.version)
                val executableName = evaluateTerraformName(terraformExtension.version)
                outputExecutable = File(buildDir, "terraform/$executableName")
            }

            copyLambdas {
                from(lambda)
                into(terraformExtension.lambdasDirectory)
            }

            sourceSets.filter { it != main }.forEach { sourceSet: TerraformSourceSet ->
                elaborateSourceSet(
                    sourceSet,
                    terraformApi,
                    terraformImplementation,
                    copyLambdas,
                    terraformExtract,
                    terraformExtension,
                    terraformInit,
                    terraformShow,
                    terraformPlan,
                    terraformApply,
                    terraformDestroyShow,
                    terraformDestroyPlan,
                    terraformDestroy,
                    softwareComponentFactory
                )
            }

        }
    }
}