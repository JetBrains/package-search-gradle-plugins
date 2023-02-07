package org.jetbrains.gradle.plugins.nativeruntime

import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.gradle.api.*
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.toolchain.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.DownloadTask
import org.jetbrains.gradle.plugins.upx.UpxPlugin
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermission.*

class AwsLambdaCustomNativeRuntimePlugin : Plugin<Project> {

    open class Extension(
        private val name: String,
        val rieEmulatorVersion: Property<String>,
        val lambdas: NamedDomainObjectContainer<LambdaExecution>,
        val awsRuntimeMainClass: Property<String>,
        val javaToolchainSpec: Property<Action<JavaToolchainSpec>>
    ) : Named {
        override fun getName() = name

        fun toolchain(action: Action<JavaToolchainSpec>) {
            javaToolchainSpec.set(action)
        }

    }

    override fun apply(target: Project): Unit = with(target) {
        apply<UpxPlugin>()
        apply(plugin = "org.graalvm.buildtools.native")
        val emulatorVersion = objects.property<String>()
            .convention("1.10")
        val awsRuntimeMainClass = objects.property<String>()
            .convention("com.amazonaws.services.lambda.runtime.api.client.AWSLambda")
        val javaToolchainSpec = objects.property<Action<JavaToolchainSpec>>()
            .convention {
                vendor.set(JvmVendorSpec.GRAAL_VM)
                languageVersion.set(JavaLanguageVersion.of(Runtime.version().feature()))
            }
        val lambdasContainer = container {
            LambdaExecution(
                name = it,
                entryClass = objects.property(),
                outputMetadataDir = objects.directoryProperty()
                    .convention(layout.buildDirectory.dir("aws/metadata/$it"))
            )
        }

        extensions.create<Extension>(
            "aws",
            "aws", emulatorVersion,
            lambdasContainer, awsRuntimeMainClass, javaToolchainSpec
        )

        val downloadLambdaRIE by tasks.registering(DownloadTask::class) {
            group = "aws"
            url.set(emulatorVersion.map { buildRieUrl(it) })
            outputFile.set(buildDir.resolve("aws/rie/runtime" + if (OperatingSystem.current().isWindows) ".exe" else ""))
            doLast {
                if (OperatingSystem.current().isUnix) {
                    Files.setPosixFilePermissions(
                        outputFile.get().asFile.toPath(),
                        setOf(OWNER_EXECUTE, OTHERS_EXECUTE, GROUP_EXECUTE)
                    )
                }
            }
        }

        val graalVmExtension = extensions.findByType<GraalVMExtension>()!!
        val sourcesSets = extensions.findByType<SourceSetContainer>()!!
        lambdasContainer.all {
            tasks.register("collect${name.capitalize()}Metadata") {
                group = "aws"
                val runtimeClasspath = sourcesSets["main"].runtimeClasspath
                dependsOn(downloadLambdaRIE, runtimeClasspath)
                outputs.dir(outputMetadataDir)
                inputs.property("entryClass", entryClass)
                doFirst {
                    exec {
                        executable = downloadLambdaRIE.get().outputFile.get().absolutePath
                        args = buildList {
                            add(
                                project.extensions
                                    .getByType<JavaToolchainService>()
                                    .launcherFor(javaToolchainSpec.get())
                                    .get()
                                    .executablePath
                                    .absolutePath
                            )
                            add(buildString {
                                append("-agentlib:native-image-agent=")
                                append("config-output-dir=${outputMetadataDir.get().asFile.absolutePath}")
                                append(",builtin-caller-filter=${graalVmExtension.agent.builtinCallerFilter.get()}")
                                append(",builtin-heuristic-filter=${graalVmExtension.agent.builtinHeuristicFilter.get()}")
                                append(",experimental-class-define-support=${graalVmExtension.agent.enableExperimentalPredefinedClasses.get()}")
                                append(",experimental-unsafe-allocation-support=${graalVmExtension.agent.enableExperimentalUnsafeAllocationTracing.get()}")
                                append(",track-reflection-metadata=${graalVmExtension.agent.trackReflectionMetadata.get()}")
                            })
                            add("-cp")
                            add(runtimeClasspath.asPath)
                            add(awsRuntimeMainClass.get())
                            add(entryClass.get())
                        }
                        println(commandLine.joinToString("\n"))
                    }
                }
            }
        }
    }
}

val RegularFile.absolutePath: String
    get() = asFile.absolutePath

class LambdaExecution(
    private val name: String,
    val entryClass: Property<String>,
    val outputMetadataDir: DirectoryProperty
) : Named {
    override fun getName() = name
}

fun isOsArm(): Boolean {
    val possibleNames = listOf("arm", "aarch")
    return possibleNames.any { it in System.getProperty("os.arch") }
}

fun buildRieUrl(version: String): String {
    val versionString = if (version == "latest") "latest" else "v$version"
    OperatingSystem.current()
    val executableName = when {
        isOsArm() -> "aws-lambda-rie-arm64"
        else -> "aws-lambda-rie"
    }
    return "https://github.com/aws/aws-lambda-runtime-interface-emulator/releases/download/$versionString/$executableName"
        .also { println(it) }
}