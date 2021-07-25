package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.gradle.plugins.terraform.tasks.TerraformExtract
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
        val terraformExtract = tasks.create<TerraformExtract>(TERRAFORM_EXTRACT_TASK_NAME)
        val ext = extensions.create<TerraformExtension>(
            TERRAFORM_EXTENSION_NAME,
            this,
            TERRAFORM_EXTENSION_NAME
        )
        afterEvaluate {
            with(terraformExtract) {
                sourceZip = resolveTerraformArchiveFile(ext.version)
                val executableName = "terraform_${ext.version}" + when {
                    OperatingSystem.current().isWindows -> ".exe"
                    else -> ""
                }
                outputExecutable = File(buildDir, "terraform/$executableName")
            }
        }
    }

    private fun Project.resolveTerraformArchiveFile(version: String): File {
        val dependency = dependencies.create(computeTerraformModuleName(version))
        val configuration = configurations.detachedConfiguration(dependency)
        configuration.isTransitive = false
        return configuration.resolve().single()
    }

    private fun computeTerraformModuleName(version: String): String {
        val currentOs: OperatingSystem = OperatingSystem.current()
        val osName = when {
            currentOs.isWindows -> "windows"
            currentOs.isMacOsX -> "darwin"
            currentOs.isLinux -> "linux"
            else -> error("OS $currentOs not supported by Terraform plugin")
        }
        val archProperty: String = System.getProperty("os.arch")
        val arch = when {
            "arm" in archProperty -> if ("64" in archProperty) "arm64" else "arm"
            "amd64" in archProperty || "x86_64" in archProperty -> "amd64"
            "386" in archProperty || "x86" in archProperty -> "386"
            else -> error("OS architecture $archProperty not supported by Terraform plugin")
        }
        return "hashicorp:terraform:$version:${osName}_$arch"
    }
}
