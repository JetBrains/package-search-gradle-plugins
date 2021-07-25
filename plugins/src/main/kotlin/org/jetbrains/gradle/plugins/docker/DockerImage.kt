package org.jetbrains.gradle.plugins.docker

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.tasks.Sync
import org.jetbrains.gradle.plugins.docker.tasks.DockerBuild
import org.jetbrains.gradle.plugins.docker.tasks.DockerPush

open class DockerImage(
    var imageName: String,
    var imageVersion: String,
    private val name: String,
    val project: Project
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
}
