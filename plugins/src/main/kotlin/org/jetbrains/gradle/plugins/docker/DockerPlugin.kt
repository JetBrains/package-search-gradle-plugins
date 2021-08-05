package org.jetbrains.gradle.plugins.docker

import org.gradle.api.*
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
        val dockerExtension = extensions.create<DockerExtension>(
            "docker",
            { path: String -> file(path) },
            "docker"
        )
        val imagesContainer = container { name ->
            DockerImage(name, project)
        }
        val repositoriesContainer = container { name ->
            DockerRegistry(name.toCamelCase())
        }

        dockerExtension.extensions.add("images", imagesContainer)
        dockerExtension.extensions.add("registries", repositoriesContainer)

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
                                repositoriesContainer.forEach { repo: DockerRegistry ->
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

                        repositoriesContainer.forEach { repo: DockerRegistry ->
                            val repoName = repo.name.toCamelCase().capitalize()
                            val dockerPushSpec: DockerPushSpec.() -> Unit = {
                                dependsOn(dockerBuild)
                                imageTag = repo.imageNamePrefix.suffixIfNot("/") + imageData.imageNameWithTag
                                imageData.tasksCustomizationContainer.pushTaskActions.executeAllOn(this)
                            }
                            val dockerPushTaskName = "push${tasksNamePrefix}To${repoName}"
                            val dockerImagePush: Provider<out DockerPushSpec> =
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
}
