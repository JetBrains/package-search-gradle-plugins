@file:Suppress("UnstableApiUsage")

package org.jetbrains.gradle.plugins.nativeruntime

import org.graalvm.buildtools.gradle.NativeImagePlugin
import org.graalvm.buildtools.gradle.dsl.GraalVMExtension
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.DownloadTask
import org.jetbrains.gradle.plugins.nativeruntime.metadata.GraalVMMetadataFiles
import org.jetbrains.gradle.plugins.upx.UpxPlugin
import org.jetbrains.gradle.plugins.upx.UpxTask
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission.*

class AwsLambdaCustomNativeRuntimePlugin : Plugin<Project> {

    abstract class Extension(
        private val name: String,
        val rieEmulatorVersion: Property<String>,
        val javaToolchainSpec: Property<Action<JavaToolchainSpec>>
    ) : Named, ExtensionAware {
        override fun getName() = name

        fun toolchain(action: Action<JavaToolchainSpec>) {
            javaToolchainSpec.set(action)
        }

    }

    object Attributes {

        const val ARTIFACT_TYPE = "aws-lambda-runtime"

    }

    override fun apply(target: Project): Unit = with(target) {
        apply<UpxPlugin>()
        apply<JavaBasePlugin>()
        apply<NativeImagePlugin>()

        val graalVmExtension = extensions.getByType<GraalVMExtension>()
        val sourcesSets = extensions.getByType<SourceSetContainer>()

        val outgoingRuntimes by configurations.creating {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(USAGE_ATTRIBUTE, objects.named(Attributes.ARTIFACT_TYPE))
            }
        }

        val emulatorVersion = objects.property<String>()
            .convention("1.10")
        val metadataDirectory = objects.directoryProperty()
            .convention(layout.projectDirectory.dir("src/main/resources/META-INF/native-image"))
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

        lambdasContainer.create("main")

        val nativeRuntimeExtension = extensions
            .create<Extension>("aws", "aws", emulatorVersion, javaToolchainSpec)

        nativeRuntimeExtension.extensions.add("lambdas", lambdasContainer)

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

        lambdasContainer.all {
            tasks.register("collect${name.capitalize()}Metadata") {
                group = "aws"
                val runtimeClasspath = sourcesSets["main"].runtimeClasspath
                dependsOn(downloadLambdaRIE, runtimeClasspath)
                outputs.dir(outputMetadataDir)
                inputs.property("entryClass", entryClass)
                doFirst {
                    exec {
                        commandLine = buildList {
                            add(downloadLambdaRIE.get().outputFile.get().absolutePath)
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
                            add(entryClass.get())
                        }
                    }
                }
            }
            val compressTask = if (name != "main") {
                graalVmExtension.binaries.create(name) {
                    classpath(sourcesSets["main"].runtimeClasspath)
                    mainClass.set(entryClass)
                    imageName.set(name)
                }
                tasks.named<UpxTask>("compressNative${name.capitalize()}Compile")
            } else {
                graalVmExtension.binaries.named("main") {
                    if (!plugins.hasPlugin(ApplicationPlugin::class.java)) {
                        mainClass.set(entryClass)
                        classpath(sourcesSets["main"].runtimeClasspath)
                    }
                }
                tasks.named<UpxTask>("compressNativeCompile")
            }
            outgoingRuntimes.outgoing {
                artifact(compressTask.flatMap { it.outputExecutable }) {
                    builtBy(compressTask)
                }
            }
            tasks.create("runLambda${name.capitalize()}") {
                dependsOn(downloadLambdaRIE, compressTask)
                group = "aws"
                doFirst {
                    exec {
                        commandLine = buildList {
                            add(downloadLambdaRIE.get().outputFile.get().absolutePath)
                            add(compressTask.get().outputExecutable.get().absolutePath)
                        }
                    }
                }
            }
        }

        tasks.register("mergeNativeAgentMetadata") {
            group = "aws"
            doFirst {
                val inputDirs = lambdasContainer.map { it.outputMetadataDir.get().asFile }
                    .plus(metadataDirectory.asFile.get())
                    .filter { it.isDirectory }
                GraalVMMetadataFiles.ALL
                    .associateWith { merger ->
                        inputDirs.mapNotNull { it.walkTopDown().firstOrNull { it.name == merger.fileName } }
                    }
                    .forEach { (merger, inputs) ->
                        merger.merge(
                            sources = inputs,
                            target = metadataDirectory.file(merger.fileName).get().asFile
                                .also { it.parentFile.mkdirs() }
                        )
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
}