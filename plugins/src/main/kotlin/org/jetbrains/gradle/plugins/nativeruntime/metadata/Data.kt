package org.jetbrains.gradle.plugins.nativeruntime.metadata

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

typealias PredefinedClassMetadata = List<PredefinedClass>

@Serializable
data class PredefinedClass(
    val type: String? = null,
    val classes: List<ClassInfo>? = null
)

@Serializable
data class ClassInfo(
    val hash: String? = null,
    val nameInfo: String? = null
)

@Serializable
data class SerializationMetadata(
    val types: List<Type>? = null,
    val lambdaCapturingTypes: List<Type>? = null,
    // I could not find any example on this property...
    val proxies: JsonArray? = null
)

@Serializable
data class Type(
    val condition: Condition? = null,
    val named: String? = null,
    val customTargetConstructorClass: String? = null
)

typealias ProxyMetadata = List<Proxy>

@Serializable
data class Proxy(
    val condition: Condition? = null,
    val interfaces: List<String>? = null
)

@Serializable
data class ResourcesMetadata(
    val resources: Resources? = null,
    val bundles: List<Bundle>? = null
)

@Serializable
data class Resources(
    val includes: List<Include>? = null,
    val exclude: List<Exclude>? = null
)

@Serializable
data class Bundle(
    val condition: Condition? = null,
    val name: String? = null,
    val locale: List<String>? = null
)

@Serializable
data class Include(
    val condition: Condition? = null,
    val pattern: String? = null
)

typealias Exclude = Include

typealias ReflectionMetadata = List<Reflection>

@Serializable
data class Reflection(
    val condition: Condition? = null,
    val name: String? = null,
    val methods: List<Method>? = null,
    val queriedMethods: List<Method>? = null,
    val fields: List<Field>? = null,
    val allDeclaredMethods: Boolean? = null,
    val allDeclaredFields: Boolean? = null,
    val allDeclaredConstructors: Boolean? = null,
    val allPublicMethods: Boolean? = null,
    val allPublicFields: Boolean? = null,
    val allPublicConstructors: Boolean? = null,
    val queryAllDeclaredMethods: Boolean? = null,
    val queryAllDeclaredConstructors: Boolean? = null,
    val queryAllPublicMethods: Boolean? = null,
    val queryAllPublicConstructors: Boolean? = null,
    val unsafeAllocated: Boolean? = null
)

typealias Field = Named
typealias JNIMetadata = ReflectionMetadata

@Serializable
data class Condition(val typeReachable: String)

@Serializable
data class Method(val name: String, val parameterTypes: List<String> = emptyList())

@Serializable
data class Named(val name: String)