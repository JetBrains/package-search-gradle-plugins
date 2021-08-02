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
import java.io.File

open class DockerImage(
    var imageName: String,
    var imageVersion: String,
    private val name: String,
    private val project: Project
) : Named {

    val imageNameWithTag
        get() = "${imageName}:${imageVersion}"

    var publicationTags = listOf<String>()

    var buildArgs: MutableMap<String, String?> = mutableMapOf()

    internal val copySpecActions: MutableList<CopySourceSpec.() -> Unit> = mutableListOf()
    internal val tasksCustomizationContainer: MutableList<Tasks.() -> Unit> = mutableListOf()

    fun files(action: CopySourceSpec.() -> Unit) {
        copySpecActions.add(action)
    }

    fun taskProviders(action: Tasks.() -> Unit) {
        tasksCustomizationContainer.add(action)
    }

    fun publicationTag(tag: String) {
        publicationTags = publicationTags + tag
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

    fun setupJvmApp(imageNameAndTag: String) =
        setupJvmApp(imageNameAndTag.substringBeforeLast(":"), imageNameAndTag.substringAfterLast(":"))

    fun setupJvmApp(imageName: String, imageTag: String) =
        setupJvmApp(JvmImageName.Custom(imageName, imageTag))

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
