@file:Suppress("FunctionName")

package org.jetbrains.gradle.plugins

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Task
import org.gradle.api.internal.resources.DefaultResourceHandler
import org.gradle.api.internal.resources.ResourceResolver
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.resources.ResourceHandler
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.property
import org.jetbrains.gradle.plugins.upx.UpxSupportedOperatingSystems
import org.jetbrains.gradle.plugins.upx.XZArchiver
import org.tukaani.xz.SeekableFileInputStream
import org.tukaani.xz.SeekableXZInputStream
import java.io.File
import java.io.OutputStream
import java.net.URI
import kotlin.properties.Delegates
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

internal operator fun <K> ListProperty<K>.setValue(parent: Any?, property: KProperty<*>, value: List<K>) {
    set(value)
}

internal operator fun <K> ListProperty<K>.getValue(parent: Any?, property: KProperty<*>): List<K> =
    get()

internal operator fun <K> SetProperty<K>.getValue(parent: Any?, property: KProperty<*>): Set<K> =
    get()

internal operator fun <K, V> MapProperty<K, V>.setValue(parent: Any?, property: KProperty<*>, value: Map<K, V>) {
    set(value)
}

internal operator fun <V> SetProperty<V>.setValue(parent: Any?, property: KProperty<*>, value: Set<V>) {
    set(value)
}

internal operator fun <K, V> MapProperty<K, V>.getValue(parent: Any?, property: KProperty<*>): Map<K, V> =
    get()

internal inline fun <reified T> ObjectFactory.property(initialValue: T) =
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
internal fun <T : Any> NamedDomainObjectContainer<T>.maybeCreating(action: T.() -> Unit) =
    ReadOnlyProperty<Any?, T> { _, property -> maybeCreate(property.name).apply(action) }

internal operator fun <T> List<T>.component6() = this[5]
internal operator fun <T> List<T>.component7() = this[6]

internal fun File.writeText(action: StringBuilder.() -> Unit) = writeText(buildString(action))

internal fun <T : Any> NamedDomainObjectContainer<T>.maybeRegister(name: String, action: T.() -> Unit) =
    if (name in names) named(name).apply { configure(action) } else register(name, action)


internal fun TaskContainer.maybeRegisterTask(name: String, action: Task.() -> Unit) =
    if (name in names) named(name).apply { configure { action(this) } } else register(name) { action(this) }

internal fun <T> Delegates.reference(initial: T) =
    observable(initial) { _, _, _ -> }

internal inline fun <T> Delegates.reference(initial: () -> T) = reference(initial())

internal fun <T : Any> T.applyAction(action: Action<T>): T {
    action.execute(this)
    return this
}

// for whatever reason the stdlib buildList is not available wtf
fun <T> buildList(builder: MutableList<T>.() -> Unit) =
    mutableListOf<T>().apply(builder).toList()

fun buildUpxFileName(version: String, platform: UpxSupportedOperatingSystems) =
    "upx-$version-${platform.fileSuffix}.${platform.extension}"

fun buildUpxUri(version: String, platform: UpxSupportedOperatingSystems) =
    URI("https://github.com/upx/upx/releases/download/v$version/${buildUpxFileName(version, platform)}")

fun File.seekable() = SeekableFileInputStream(this)

fun SeekableFileInputStream.lzma2() = SeekableXZInputStream(this)

fun ResourceHandler.xz(path: Any): XZArchiver {
    val resourceResolverField = DefaultResourceHandler::class.java.getDeclaredField("resourceResolver")
    resourceResolverField.isAccessible = true
    val resourceResolver = resourceResolverField.get(this) as ResourceResolver
    val resource = resourceResolver.resolveResource(path)
    resourceResolverField.isAccessible = false
    return XZArchiver(resource)
}