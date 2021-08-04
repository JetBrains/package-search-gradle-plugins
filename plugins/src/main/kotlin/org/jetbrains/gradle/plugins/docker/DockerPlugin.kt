package org.jetbrains.gradle.plugins.docker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.*
import org.jetbrains.gradle.plugins.docker.tasks.*
import java.io.File

open class DockerPlugin : Plugin<Project> {

    companion object {

        const val TASK_GROUP = "docker"
    }

    override fun apply(target: Project): Unit = with(target) {
        val dockerExtension = extensions.create<DockerExtension>("docker", project, "docker")
        val imagesContainer = container { name ->
            DockerImage(name, project)
        }
        val repositoriesContainer = container { name ->
            DockerRepository(name.toCamelCase())
        }

        dockerExtension.extensions.add("images", imagesContainer)
        dockerExtension.extensions.add("repositories", repositoriesContainer)

        imagesContainer.register(project.name.toCamelCase())

        val dockerPrepare by tasks.creating {
            group = TASK_GROUP
        }
        val dockerBuild by tasks.creating {
            dependsOn(dockerPrepare)
            group = TASK_GROUP
        }
        tasks.maybeCreate("build").dependsOn(dockerBuild)
        val dockerPush by tasks.creating {
            dependsOn(dockerBuild)
            group = TASK_GROUP
        }
        afterEvaluate {
            if (imagesContainer.isNotEmpty() && isDockerPresent(dockerExtension.remoteConfigBuilder))
                project.tasks {
                    imagesContainer.forEach { imageData: DockerImage ->

                        val tasksNamePrefix = imageData.name.toCamelCase().capitalize()

                        val dockerImagePrepare = register<Sync>("docker${tasksNamePrefix}Prepare") {
                            imageData.copySpecActions.executeAllOn(this)
                            into(File(buildDir, "docker/${tasksNamePrefix.decapitalize()}"))
                            imageData.tasksCustomizationContainer.prepareTaskActions.executeAllOn(this)
                        }
                        dockerPush.dependsOn(dockerImagePrepare)
                        val dockerBuildTaskName = "docker${tasksNamePrefix}Build"
                        val buildSpec: DockerBuildSpec.() -> Unit = {
                            dependsOn(dockerImagePrepare)
                            tags = buildList {
                                add(imageData.imageNameWithTag)
                                repositoriesContainer.forEach { repo: DockerRepository ->
                                    add(repo.imageNamePrefix.suffixIfNot("/") + imageData.imageNameWithTag)
                                }
                            }
                            contextFolder = dockerImagePrepare.get().destinationDir
                            buildArgs = imageData.buildArgs
                            imageData.tasksCustomizationContainer.buildTaskActions.executeAllOn(this)
                        }
                        val dockerImageBuild: TaskProvider<out Task> =
                            dockerExtension.remoteConfigBuilder?.let { remoteConfig ->
                                registerDockerBuild(dockerBuildTaskName, remoteConfig, buildSpec)
                            } ?: register<DockerExecBuild>(dockerBuildTaskName, buildSpec)

                        dockerBuild.dependsOn(dockerImageBuild)

                        repositoriesContainer.forEach { repo: DockerRepository ->
                            val repoName = repo.name.toCamelCase().capitalize()
                            val dockerPushSpec: DockerPushSpec.() -> Unit = {
                                dependsOn(dockerBuild)
                                imageTag = repo.imageNamePrefix.suffixIfNot("/") + imageData.imageNameWithTag
                                imageData.tasksCustomizationContainer.pushTaskActions.executeAllOn(this)
                            }
                            val dockerPushTaskName = "docker${tasksNamePrefix}${repoName}Push"
                            val dockerImagePush: Provider<out DockerPushSpec>? =
                                dockerExtension.remoteConfigBuilder?.let { remoteConfig ->
                                    registerDockerPush(
                                        dockerPushTaskName,
                                        dockerImageBuild,
                                        repo,
                                        imageData,
                                        remoteConfig
                                    )
                                } ?: registerDockerExecPush(repoName, repo, dockerPushTaskName, dockerPushSpec)
                            dockerPush.dependsOn(dockerImagePush)
                        }
                    }
                }
        }
    }

    private fun Project.isDockerPresent(remoteConfigBuilder: DockerExtension.Remote?) =
        runCatching {
            remoteConfigBuilder?.let { remoteConfig ->
                buildDockerHttpClient(null, remoteConfig).pingCmd().exec()
            } ?: exec {
                    executable = "docker"
                    args = listOf("version")
                    standardOutput = NullOutputStream()
                    errorOutput = NullOutputStream()
            }
        }
            .onFailure { logger.warn("Docker not available: ${it.message}") }
            .map { true }
            .getOrDefault(false)

    private fun TaskContainerScope.registerDockerBuild(
        dockerBuildTaskName: String,
        remoteConfig: DockerExtension.Remote,
        buildSpec: DockerBuildSpec.() -> Unit
    ) = register<DockerBuild>(dockerBuildTaskName) {
        client = buildDockerHttpClient(null, remoteConfig)
        buildSpec()
    }

    private fun TaskContainerScope.registerDockerPush(
        dockerPushTaskName: String,
        dockerImageBuild: TaskProvider<out Task>,
        repo: DockerRepository,
        imageData: DockerImage,
        remoteConfig: DockerExtension.Remote
    ) = register<DockerPush>(dockerPushTaskName) {
        dependsOn(dockerImageBuild)
        imageTag = repo.imageNamePrefix.suffixIfNot("/") + imageData.imageNameWithTag
        client = buildDockerHttpClient(repo, remoteConfig)
    }

    private fun TaskContainerScope.registerDockerExecPush(
        repoName: String,
        repo: DockerRepository,
        dockerPushTaskName: String,
        dockerPushSpec: DockerPushSpec.() -> Unit
    ): TaskProvider<DockerExecPush> {
        val dockerLoginTaskName = "docker${repoName}Login"
        val dockerLogin =
            register<DockerExecLogin>(dockerLoginTaskName) {
                url = repo.url
                username = repo.username.takeIf { it.isNotEmpty() }
                    ?: repo.email.takeIf { it.isNotEmpty() }
                            ?: error("Docker repository \"${repo.name}\" has empty username and email")
                password = repo.password
            }
        return register<DockerExecPush>(dockerPushTaskName) {
            dependsOn(dockerLogin)
            dockerPushSpec()
        }
    }
}
