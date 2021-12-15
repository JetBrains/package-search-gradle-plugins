package org.jetbrains.gradle.plugins.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.TaskContainerScope
import org.gradle.kotlin.dsl.register
import org.jetbrains.gradle.plugins.NullOutputStream
import org.jetbrains.gradle.plugins.docker.tasks.*
import org.jetbrains.gradle.plugins.suffixIfNot

internal fun Project.buildDockerHttpClient(
    repo: DockerRegistryCredentials?,
    dockerRemoteConfig: DockerExtension.Remote
): DockerClient {
    val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .apply {
            withDockerHost(dockerRemoteConfig.host)
            withDockerTlsVerify(dockerRemoteConfig.useTsl)
            if (repo != null) {
                dockerRemoteConfig.dockerCertPath?.let { withDockerCertPath(it.absolutePath) }
                withRegistryUrl(repo.url.takeIf { it.isNotEmpty() }
                    ?: error("Docker Repository ${repo.name} has an empty url"))
                repo.username?.takeIf { it.isNotEmpty() }?.let { withRegistryUsername(it) }
                withRegistryPassword(repo.password)
                repo.email?.takeIf { it.isNotEmpty() }?.let { withRegistryEmail(it) }
                if (repo.username == null && repo.email == null)
                    logger.warn("Username and email for registry ${repo.name} are both null, skipping login.")
            }
        }
        .build()
    val httpClient = ApacheDockerHttpClient.Builder().apply {
        dockerHost(config.dockerHost)
        sslConfig(config.sslConfig)
    }.build()
    return DockerClientImpl.getInstance(config, httpClient)
}

internal fun Project.isDockerPresent(remoteConfigBuilder: DockerExtension.Remote?) =
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

internal fun TaskContainerScope.registerDockerBuild(
    dockerBuildTaskName: String,
    remoteConfig: DockerExtension.Remote,
    buildSpec: DockerBuildSpec.() -> Unit
) = register<DockerBuild>(dockerBuildTaskName) {
    client = project.buildDockerHttpClient(null, remoteConfig)
    buildSpec()
}

internal fun TaskContainerScope.registerDockerPush(
    dockerPushTaskName: String,
    dockerImageBuild: TaskProvider<out Task>,
    repo: DockerRegistryCredentials,
    imageData: DockerImage,
    remoteConfig: DockerExtension.Remote
) = register<DockerPush>(dockerPushTaskName) {
    dependsOn(dockerImageBuild)
    imageTag = repo.imageNamePrefix.suffixIfNot("/") + imageData.imageNameWithTag
    client = project.buildDockerHttpClient(repo, remoteConfig)
}

internal fun TaskContainerScope.registerDockerExecPush(
    repoName: String,
    repo: DockerRegistryCredentials,
    dockerPushTaskName: String,
    dockerPushSpec: DockerPushSpec.() -> Unit,
    rootProject: Project
): TaskProvider<DockerExecPush> {
    val dockerLoginTaskName = "docker${repoName}Login"
    val dockerLogin =
        rootProject.tasks.findByName(dockerLoginTaskName) as? DockerExecLogin
            ?: rootProject.tasks.register<DockerExecLogin>(dockerLoginTaskName) {
                url = repo.url
                username = repo.username?.takeIf { it.isNotEmpty() } ?: repo.email?.takeIf { it.isNotEmpty() }
                password = repo.password
            }
    return register<DockerExecPush>(dockerPushTaskName) {
        dependsOn(dockerLogin)
        dockerPushSpec()
    }
}
