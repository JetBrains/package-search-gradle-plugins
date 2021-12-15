package org.jetbrains.gradle.plugins.terraform

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCopyDetails
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
        listOf("arm", "aarch").any { it in archProperty } -> if ("64" in archProperty) "arm64" else "arm"
        "amd64" in archProperty || "x86_64" in archProperty -> "amd64"
        "386" in archProperty || "x86" in archProperty -> "386"
        else -> error("OS architecture \"$archProperty\" not supported by Terraform plugin")
    }
    return "hashicorp:terraform:$version:${osName}_$arch"
}

internal fun Project.generateTerraformDetachedConfiguration(version: String): Configuration {
    val dependency = dependencies.create(computeTerraformModuleName(version))
    return configurations.detachedConfiguration(dependency)
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

internal fun FileCopyDetails.resolveModules(
    availableModules: Set<TerraformModuleMetadata>
) = filter { line ->

    val hasSourceKeyword = "source" in line
    val substring1 by lazy { line.substringAfter("source") }
    val hasEqualsAfterSource by lazy { "=" in substring1 }
    val substring2 by lazy { substring1.substringAfter("=") }
    val hasModulesKeyword by lazy { "modules." in substring2 }
    val substring3 by lazy { substring2.substringAfter("modules.").replace(".", "/") }
    val moduleMatch by lazy { availableModules.find { it.asPath in substring3 } }

    if (hasSourceKeyword && hasEqualsAfterSource && hasModulesKeyword && moduleMatch != null)
        buildString {
            append(line.substringBefore("source"))
            append("source = ")
            append('"')
            when (relativePath.segments.size) {
                1 -> append("./${moduleMatch!!.asPath}")
                else -> {
                    val modulePathSegments = moduleMatch!!.asPath
                        .split("/").toTypedArray()

                    val uncommonElements: Array<String> = relativePath.parent.segments
                        .uncommonElementsFromLeft(modulePathSegments)

                    repeat(uncommonElements.size) { append("../") }

                    append(
                        modulePathSegments.uncommonElementsFromLeft(relativePath.parent.segments)
                            .joinToString("/")
                    )
                }
            }
            append('"')
        } else line
}
