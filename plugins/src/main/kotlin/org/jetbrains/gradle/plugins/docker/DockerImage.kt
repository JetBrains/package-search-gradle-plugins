package org.jetbrains.gradle.plugins.docker

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import org.jetbrains.gradle.plugins.docker.tasks.DockerBuild
import org.jetbrains.gradle.plugins.docker.tasks.DockerPush
import org.jetbrains.gradle.plugins.docker.tasks.GenerateJvmAppDockerfile
import org.jetbrains.gradle.plugins.has
import org.jetbrains.gradle.plugins.toKebabCase
import java.io.File

/**
 * Data model for a Docker Image. This class store all the
 * information necessary to build a Docker Image.
 *
 * @param imageName Name of the image.
 * @param imageVersion Version of the image, defaults to `project.version`.
 */
data class DockerImage(
    private val name: String,
    private val project: Project,
    var imageName: String = name.toKebabCase(),
    var imageVersion: String = project.version as String,
) : Named {

    /**
     * Full name of the image, ex: openjdk:11
     */
    val imageNameWithTag
        get() = "${imageName}:${imageVersion}"

    /**
     * Argument passed during image building.
     */
    var buildArgs: MutableMap<String, String> = mutableMapOf()

    internal val copySpecActions: MutableList<CopySourceSpec.() -> Unit> = mutableListOf()
    internal val tasksCustomizationContainer = Tasks()

    /**
     * Creates a [CopySourceSpec] to create the folder in which
     * execute `docekr build`
     */
    fun files(action: CopySourceSpec.() -> Unit) {
        copySpecActions.add(action)
    }

    /**
     * Accesses all generated tasks for this [DockerImage].
     */
    fun taskProviders(action: Tasks.() -> Unit) {
        tasksCustomizationContainer.apply(action)
    }

    open class Tasks {

        internal val prepareTaskActions: MutableList<Sync.() -> Unit> = mutableListOf()

        internal val buildTaskActions: MutableList<DockerBuild.() -> Unit> = mutableListOf()

        internal val pushTaskActions: MutableList<DockerPush.() -> Unit> = mutableListOf()

        fun dockerPrepareTaskProvider(action: Sync.() -> Unit) {
            prepareTaskActions.add(action)
        }

        fun dockerBuildTaskProvider(action: DockerBuild.() -> Unit) {
            buildTaskActions.add(action)
        }

        fun dockerPushTaskProvider(action: DockerPush.() -> Unit) {
            pushTaskActions.add(action)
        }
    }

    override fun getName() = name

    /**
     * Sets up this image to run a JVM application using the given [imageNameAndTag] as
     * base image. It requires the [ApplicationPlugin] to be applied to the project correctly.
     */
    fun setupJvmApp(imageNameAndTag: String) =
        setupJvmApp(imageNameAndTag.substringBeforeLast(":"), imageNameAndTag.substringAfterLast(":"))

    /**
     * Sets up this image to run a JVM application using the given [imageName]:[imageTag]
     * as base image. It requires the [ApplicationPlugin] to be applied to the project correctly.
     */
    fun setupJvmApp(imageName: String, imageTag: String) =
        setupJvmApp(JvmImageName.Custom(imageName, imageTag))

    /**
     * Sets up this image to run a JVM application using the given [baseImage].
     * It requires the [ApplicationPlugin] to be applied to the project correctly.
     */
    fun setupJvmApp(baseImage: JvmImageName = JvmImageName.OpenJRE8Slim) {
        if (!project.plugins.has<ApplicationPlugin>()) {
            project.logger.error("Application plugin not applied. Add 'id(\"application\")' in the 'plugins { }' block.")
            return
        }
        if (!project.the<JavaApplication>().mainClass.isPresent) {
            project.logger.error("Application main class not set. Please set the \"application.mainClass\" property.")
            return
        }
        val baseImageTaskNotation = baseImage.toString().split(":")
            .flatMap { it.split("-") }.joinToString("") { it.capitalize() }
        val generateDockerfileTaskName = "generate${baseImageTaskNotation}JvmAppDockerfile"
        val extractImageTask =
            project.tasks.findByName(generateDockerfileTaskName) as? GenerateJvmAppDockerfile
                ?: project.tasks.create<GenerateJvmAppDockerfile>(generateDockerfileTaskName) {
                    this.baseImage = baseImage
                    val baseFileName = baseImage.toString().replace(":", "-")
                    outputDockerfile = File(outputDockerfile.parentFile, "$baseFileName-app-dockerfile")
                    appName = project.name
                }
        files {
            from(project.tasks.named<Sync>(DistributionPlugin.TASK_INSTALL_NAME))
            from(extractImageTask) {
                rename { "Dockerfile" }
            }
        }
    }
}
