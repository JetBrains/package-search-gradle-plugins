@file:Suppress("FunctionName")

package org.jetbrains.gradle.plugins

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.property
import java.io.File
import java.io.OutputStream
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Returns true if the container has a plugin with the given type [T], false otherwise.
 *
 * @param [T] The type of the plugin
 */
internal inline fun <reified T : Plugin<*>> PluginContainer.has() =
    hasPlugin(T::class.java)

internal fun <K> Iterable<K.() -> Unit>.executeAllOn(context: K) =
    forEach { action -> action(context) }

@JvmName("executeAllActionsOn")
internal fun <K> Iterable<Action<K>>.executeAllOn(context: K) =
    forEach { action -> action(context) }

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
internal operator fun <K> SetProperty<K>.getValue(parent: Any?, property: KProperty<*>): Set<K> =
    get()

@Suppress("UnstableApiUsage")
internal operator fun <K, V> MapProperty<K, V>.setValue(parent: Any?, property: KProperty<*>, value: Map<K, V>) {
    set(value)
}

@Suppress("UnstableApiUsage")
internal operator fun <V> SetProperty<V>.setValue(parent: Any?, property: KProperty<*>, value: Set<V>) {
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
        char.isUpperCase() -> "-${char.toString().toLowerCase()}"
        char.isLetter() -> char
        else -> if (includeSymbols) char else '-'
    }
}.joinToString("")

internal inline fun <reified T : Any> ObjectFactory.nullableProperty() =
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

fun NullOutputStream() = OutputStream { }
internal val <T : Any> NamedDomainObjectContainer<T>.maybeCreating
    get() = ReadOnlyProperty<Any?, T> { _, property -> maybeCreate(property.name) }

internal fun <T : Any> NamedDomainObjectContainer<T>.maybeCreating(action: T.() -> Unit) =
    ReadOnlyProperty<Any?, T> { _, property -> maybeCreate(property.name).apply(action) }

internal operator fun <T> List<T>.component6() = this[5]
internal operator fun <T> List<T>.component7() = this[6]

internal fun File.writeText(action: StringBuilder.() -> Unit) = writeText(buildString(action))

internal fun StringBuilder.appendLine(text: String) = append(text + "\n")
