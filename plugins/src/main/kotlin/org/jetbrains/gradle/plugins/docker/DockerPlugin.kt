package org.jetbrains.gradle.plugins.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.core.exec.PingCmdExec
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.applyAll
import org.jetbrains.gradle.plugins.docker.tasks.DockerBuild
import org.jetbrains.gradle.plugins.docker.tasks.DockerPush
import org.jetbrains.gradle.plugins.executeAllOn
import org.jetbrains.gradle.plugins.suffixIfNot
import java.io.File

open class DockerPlugin : Plugin<Project> {

    companion object {

        const val TASK_GROUP = "docker"
    }

    override fun apply(target: Project): Unit = with(target) {
        val ext = extensions.create<DockerExtension>("docker", project, "docker")
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
            val clientBuilder: (DockerRepository?) -> DockerClient = { repo ->
                val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .apply {
                        withDockerHost(ext.host)
                        withDockerTlsVerify(ext.useTsl)
                        if (repo != null) {
                            ext.dockerCertPath?.let { withDockerCertPath(it.absolutePath) }
                            withRegistryUrl(repo.url.takeIf { it.isNotEmpty() }
                                ?: error("Docker Repository ${repo.name} has an empty url"))
                            repo.username.takeIf { it.isNotEmpty() }?.let { withRegistryUsername(it) }
                            withRegistryPassword(repo.password)
                            repo.email.takeIf { it.isNotEmpty() }?.let { withRegistryEmail(it) }
                        }
                    }
                    .build()
                    val httpClient = ApacheDockerHttpClient.Builder().apply {
                        dockerHost(config.dockerHost)
                        sslConfig(config.sslConfig)
                    }.build()
                DockerClientImpl.getInstance(config, httpClient)
            }

            val isDockerPresent = runCatching { clientBuilder(null).pingCmd().exec() }
                .onFailure { logger.warn("Docker not available: ${it.message}") }
                .map { true }
                .getOrDefault(false)

            if (ext.images.isNotEmpty() && isDockerPresent) project.tasks {
                ext.images.forEach { imageData: DockerImage ->

                    val tasksNamePrefix = imageData.name.split("-").joinToString("") { it.capitalize() }
                    val tasksCustomActions = DockerImage.Tasks().applyAll(imageData.tasksCustomizationContainer)

                    val dockerImagePrepare = register<Sync>("docker${tasksNamePrefix}Prepare") {
                        imageData.copySpecActions.executeAllOn(this)
                        into(File(buildDir, "docker/${tasksNamePrefix.decapitalize()}"))
                        tasksCustomActions.prepareTaskActions.executeAllOn(this)
                    }
                    dockerPush.dependsOn(dockerImagePrepare)

                    val dockerImageBuild = register<DockerBuild>("docker${tasksNamePrefix}Build") {
                        dependsOn(dockerImagePrepare)
                        tags = buildList {
                            add(imageData.imageNameWithTag)
                            ext.repositories.forEach { repo: DockerRepository ->
                                add(repo.imageNamePrefix.suffixIfNot("/") + imageData.imageNameWithTag)
                            }
                        }
                        contextFolder = dockerImagePrepare.get().destinationDir
                        buildArgs = imageData.buildArgs
                        client = clientBuilder(null)
                        tasksCustomActions.buildTaskActions.executeAllOn(this)
                    }
                    dockerBuild.dependsOn(dockerImageBuild)

                    ext.repositories.forEach { repo: DockerRepository ->
                        val repoName = repo.name.capitalize()
                            .replace("-", "").replace("_", "")
                        val dockerImagePush = register<DockerPush>("docker${tasksNamePrefix}${repoName}Push") {
                            imageTag = repo.imageNamePrefix.suffixIfNot("/") + imageData.imageNameWithTag
                            client = clientBuilder(repo)
                        }
                        dockerPush.dependsOn(dockerImagePush)
                    }
                }
            }
        }
    }
}
