package org.jetbrains.gradle.plugins.nativeruntime.metadata

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import java.io.File

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
        output.writeText(json.encodeToString(inputs.flatten().distinct()))
    }

    val PREDEFINED_CLASSES = merger("predefined-classes-config.json") { inputFiles, output ->
        val inputs = inputFiles.map { json.decodeFromString<PredefinedClassMetadata>(it.readText()) }
        output.writeText(json.encodeToString(inputs.flatten().distinct()))
    }

    val PROXY = merger("proxy-config.json") { inputFiles, output ->
        val inputs = inputFiles.map { json.decodeFromString<ProxyMetadata>(it.readText()) }
        output.writeText(json.encodeToString(inputs.flatten().distinct()))
    }

    val REFLECT = merger("reflect-config.json") { inputFiles, output ->
        val inputs = inputFiles.map { json.decodeFromString<ReflectionMetadata>(it.readText()) }
        output.writeText(json.encodeToString(inputs.flatten().distinct()))
    }

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