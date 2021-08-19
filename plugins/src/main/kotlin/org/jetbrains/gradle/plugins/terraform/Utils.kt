package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.internal.os.OperatingSystem

internal fun evaluateTerraformName(version: String) =
    "terraform_$version" + when {
        OperatingSystem.current().isWindows -> ".exe"
        else -> ""
    }

internal fun computeTerraformModuleName(version: String): String {
    val currentOs: OperatingSystem = OperatingSystem.current()
    val osName = when {
        currentOs.isWindows -> "windows"
        currentOs.isMacOsX -> "darwin"
        currentOs.isLinux -> "linux"
        else -> error("OS \"$currentOs\" not supported by Terraform plugin")
    }
    val archProperty: String = System.getProperty("os.arch")
    val arch = when {
        "arm" in archProperty -> if ("64" in archProperty) "arm64" else "arm"
        "amd64" in archProperty || "x86_64" in archProperty -> "amd64"
        "386" in archProperty || "x86" in archProperty -> "386"
        else -> error("OS architecture \"$archProperty\" not supported by Terraform plugin")
    }
    return "hashicorp:terraform:$version:${osName}_$arch"
}

internal fun Project.generateTerraformDetachedConfiguration(version: String): Configuration {
    val dependency = dependencies.create(computeTerraformModuleName(version))
    val configuration = configurations.detachedConfiguration(dependency)
    configuration.isTransitive = false
    return configuration
}

internal fun TerraformSourceSet.getSourceDependencies(): Set<TerraformSourceSet> {
    val visited = mutableSetOf<TerraformSourceSet>()
    val queue = dependsOn.toMutableList()
    while (queue.isNotEmpty()) {
        val currentSourceSet = queue.removeAt(0)
        visited.add(currentSourceSet)
        queue.addAll(currentSourceSet.dependsOn.filter { it !in visited })
    }
    return visited
}

internal operator fun <T : Task> T.invoke(action: T.() -> Unit) = action(this)
