package org.jetbrains.gradle.plugins

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.docker.DockerImage
import org.jetbrains.gradle.plugins.docker.DockerRepository
import org.jetbrains.gradle.plugins.docker.JvmImageName
import org.jetbrains.gradle.plugins.docker.tasks.GenerateJvmAppDockerfile
import java.io.File
import kotlin.reflect.KProperty

typealias DockerImagesContainer = NamedDomainObjectContainer<DockerImage>
typealias DockerRepositoriesContainer = NamedDomainObjectContainer<DockerRepository>

/**
 * Returns true if the container has a plugin with the given type [T], false otherwise.
 *
 * @param [T] The type of the plugin
 */
internal inline fun <reified T : Plugin<*>> PluginContainer.has() =
    hasPlugin(T::class.java)

internal fun <K> Iterable<K.() -> Unit>.executeAllOn(context: K) =
    forEach { action -> action(context) }

internal fun <K> K.applyAll(actions: Iterable<K.() -> Unit>): K {
    actions.forEach { apply(it) }
    return this
}

@JvmName("executeAllActionsOn")
internal fun <K> Iterable<Action<K>>.executeAllOn(context: K) =
    forEach { action -> action(context) }

internal inline fun <reified T : Any> ExtensionContainer.create(name: String, action: T.() -> Unit): T =
    create<T>(name).apply(action)

fun DockerImage.setupJvmApp(imageName: String, imageTag: String) =
    setupJvmApp(JvmImageName.Custom(imageName, imageTag))

fun DockerImage.setupJvmApp(baseImage: JvmImageName = JvmImageName.OpenJRE8Slim) {
    require(project.plugins.has<ApplicationPlugin>()) {
        "Application plugin not applied. add 'id(\"application\")' in the 'plugins { }' block."
    }
    require(project.the<JavaApplication>().mainClass.isPresent) {
        "Application main class not set. Please set the \"application.mainClass\" property."
    }
    val baseImageTaskNotation = baseImage.toString().split(":")
        .flatMap { it.split("-") }.joinToString("") { it.capitalize() }
    val generateDockerfileTaskName = "generate${baseImageTaskNotation}JvmAppDockerfile"
    val extractImageTask = project.tasks.findByName(generateDockerfileTaskName)
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

internal fun <E> MutableCollection<E>.addAll(vararg elements: E) =
    elements.forEach { add(it) }

@Suppress("UnstableApiUsage")
internal operator fun <K> ListProperty<K>.setValue(parent: Any?, property: KProperty<*>, value: List<K>) {
    set(value)
}

@Suppress("UnstableApiUsage")
internal operator fun <K> ListProperty<K>.getValue(parent: Any?, property: KProperty<*>): List<K> =
    get()

@Suppress("UnstableApiUsage")
internal operator fun <K, V> MapProperty<K, V>.setValue(parent: Any?, property: KProperty<*>, value: Map<K, V>) {
    set(value)
}

@Suppress("UnstableApiUsage")
internal operator fun <K, V> MapProperty<K, V>.getValue(parent: Any?, property: KProperty<*>): Map<K, V> =
    get()

internal inline fun <reified T> ObjectFactory.propertyWithDefault(initialValue: T) =
    property<T>().apply { set(initialValue) }

internal fun String.suffixIfNot(s: String) =
    if (endsWith(s)) this else this + s
