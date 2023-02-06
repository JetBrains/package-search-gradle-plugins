package org.jetbrains.gradle.plugins.upx

import org.gradle.api.internal.file.archive.compression.URIBuilder
import org.gradle.api.resources.ReadableResource
import org.gradle.api.resources.internal.ReadableResourceInternal
import org.jetbrains.gradle.plugins.lzma2
import org.jetbrains.gradle.plugins.seekable
import java.io.InputStream
import java.net.URI

class XZArchiver(private val resource: ReadableResourceInternal) : ReadableResource {

    private val uri: URI = URIBuilder(resource.uri).schemePrefix("xz:").build()

    override fun getDisplayName() = resource.displayName

    override fun getURI() = uri

    override fun getBaseName() = displayName

    override fun read(): InputStream = resource.backingFile.seekable().lzma2()

    override fun toString() = displayName

}