package org.jetbrains.gradle.plugins.nativeruntime.metadata

import java.io.File

internal fun merger(fileName: String, merge: (List<File>, File) -> Unit) =
    object : GraalVMMetadataFiles.Merger {
        override val fileName = fileName
        override fun merge(sources: List<File>, target: File) = merge(sources, target)
    }