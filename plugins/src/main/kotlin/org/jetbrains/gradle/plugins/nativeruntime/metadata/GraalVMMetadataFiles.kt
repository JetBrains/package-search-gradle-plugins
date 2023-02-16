package org.jetbrains.gradle.plugins.nativeruntime.metadata

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import java.io.File

fun GraalVMMetadataFiles.Merger.copy(fileName: String) = object : GraalVMMetadataFiles.Merger {
    override val fileName = fileName
    override fun merge(sources: List<File>, target: File) = this@copy.merge(sources, target)
}

object GraalVMMetadataFiles {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
    }

    interface Merger {

        val fileName: String
        fun merge(sources: List<File>, target: File)
    }

    val JNI = merger("jni-config.json") { inputFiles, output ->
        val inputs = inputFiles.map { json.decodeFromString<JNIMetadata>(it.readText()) }
            .flatten()
            .groupBy { it.name to it.condition }
            .map { (name, reflections) ->
                Reflection(
                    name = name.first,
                    condition = name.second,
                    methods = reflections.mapNotNull { it.methods }.flatten().distinct().takeIf { it.isNotEmpty() },
                    queriedMethods = reflections.mapNotNull { it.queriedMethods }.flatten().distinct().takeIf { it.isNotEmpty() },
                    fields = reflections.mapNotNull { it.fields }.flatten().distinct().takeIf { it.isNotEmpty() },
                    allDeclaredMethods = reflections.any { it.allDeclaredMethods ?: false }.takeIf { it },
                    allDeclaredFields = reflections.any { it.allDeclaredFields ?: false }.takeIf { it },
                    allDeclaredConstructors = reflections.any { it.allDeclaredConstructors ?: false }.takeIf { it },
                    allPublicMethods = reflections.any { it.allPublicMethods ?: false }.takeIf { it },
                    allPublicFields = reflections.any { it.allPublicFields ?: false }.takeIf { it },
                    allPublicConstructors = reflections.any { it.allPublicConstructors ?: false }.takeIf { it },
                    queryAllDeclaredMethods = reflections.any { it.queryAllDeclaredMethods ?: false }.takeIf { it },
                    queryAllDeclaredConstructors = reflections.any { it.queryAllDeclaredConstructors ?: false }.takeIf { it },
                    queryAllPublicMethods = reflections.any { it.queryAllPublicMethods ?: false }.takeIf { it },
                    queryAllPublicConstructors = reflections.any { it.queryAllPublicConstructors ?: false }.takeIf { it },
                    unsafeAllocated = reflections.any { it.unsafeAllocated ?: false }.takeIf { it }
                )
            }
        output.writeText(json.encodeToString(inputs))
    }

    val PREDEFINED_CLASSES = merger("predefined-classes-config.json") { inputFiles, output ->
        val inputs = inputFiles.map { json.decodeFromString<PredefinedClassMetadata>(it.readText()) }
            .flatten()
            .groupBy { it.type }
            .map { (type, classes) ->
                PredefinedClass(
                    type = type,
                    classes = classes.map { it.classes }.flatten().distinct()
                )
            }

        output.writeText(json.encodeToString(inputs))
    }

    val PROXY = merger("proxy-config.json") { inputFiles, output ->
        val inputs = inputFiles.map { json.decodeFromString<ProxyMetadata>(it.readText()) }
            .flatten()
            .groupBy { it.condition }
            .map { (condition, proxies) ->
                Proxy(
                    condition = condition,
                    interfaces = proxies.flatMap { it.interfaces }.distinct()
                )
            }
        output.writeText(json.encodeToString(inputs))
    }

    val REFLECT = JNI.copy("reflect-config.json")

    val RESOURCE = merger("resource-config.json") { inputFiles, output ->
        val inputs = inputFiles.map { json.decodeFromString<ResourcesMetadata>(it.readText()) }
        val resources = ResourcesMetadata(
            Resources(
                includes = inputs.mapNotNull { it.resources?.includes }.flatten().distinct().takeIf { it.isNotEmpty() },
                exclude = inputs.mapNotNull { it.resources?.exclude }.flatten().distinct().takeIf { it.isNotEmpty() },
            ),
            bundles = inputs.mapNotNull { it.bundles }.flatten().distinct().takeIf { it.isNotEmpty() }
        )
        output.writeText(json.encodeToString(resources))
    }

    val SERIALIZATION = merger("serialization-config.json") { inputFiles, output ->
        val inputs = inputFiles.map { json.decodeFromString<SerializationMetadata>(it.readText()) }
        val serialization = SerializationMetadata(
            types = inputs.map { it.types }.flatten().distinct(),
            lambdaCapturingTypes = inputs.map { it.lambdaCapturingTypes }.flatten().distinct(),
            proxies = JsonArray(inputs.mapNotNull { it.proxies }.flatten())
        )
        output.writeText(json.encodeToString(serialization))
    }

    val ALL = listOf(JNI, PREDEFINED_CLASSES, PROXY, REFLECT, RESOURCE, SERIALIZATION)
}