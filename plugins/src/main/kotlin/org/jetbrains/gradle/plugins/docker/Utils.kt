package org.jetbrains.gradle.plugins.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient

internal fun buildDockerHttpClient(repo: DockerRepository?, dockerRemoteConfig: DockerExtension.Remote): DockerClient {
    val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .apply {
            withDockerHost(dockerRemoteConfig.host)
            withDockerTlsVerify(dockerRemoteConfig.useTsl)
            if (repo != null) {
                dockerRemoteConfig.dockerCertPath?.let { withDockerCertPath(it.absolutePath) }
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
    return DockerClientImpl.getInstance(config, httpClient)
}
