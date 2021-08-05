@file:Suppress("FunctionName")

package org.jetbrains.gradle.plugins

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import org.jetbrains.gradle.plugins.docker.DockerImage
import org.jetbrains.gradle.plugins.docker.DockerRegistryCredentials
import java.io.OutputStream
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

typealias DockerImagesContainer = NamedDomainObjectContainer<DockerImage>
typealias DockerRepositoriesContainer = NamedDomainObjectContainer<DockerRegistryCredentials>

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

internal fun String.toCamelCase() =
    replace(Regex("[^a-zA-Z\\d](\\w)")) { it.value.last().toUpperCase().toString() }

internal fun String.fromCamelCaseToKebabCase(includeSymbols: Boolean = false) = map { char ->
    when {
        char.isUpperCase() -> "-${char.lowercase()}"
        char.isLetter() -> char
        else -> if (includeSymbols) char else '-'
    }
}.joinToString("")

internal inline fun <reified T : Any> ObjectFactory.nullableProperty()=
    property<T>().nullable()

fun <T> Property<T>.nullable() = NullableProperty(this)

class NullableProperty<T>(private val gradleProperty: Property<T>) : ReadWriteProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? =
        gradleProperty.orNull

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        gradleProperty.set(value)
    }
}

fun OutputStream(action: (Byte) -> Unit) = object : OutputStream() {
    override fun write(b: Int) = action(b.toByte())
}

fun NullOutputStream() = OutputStream {  }
