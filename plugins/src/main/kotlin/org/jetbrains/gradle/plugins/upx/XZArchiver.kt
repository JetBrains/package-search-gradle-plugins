package org.jetbrains.gradle.plugins.upx

import org.gradle.api.internal.file.archive.compression.URIBuilder
import org.gradle.api.resources.internal.ReadableResourceInternal
import org.jetbrains.gradle.plugins.lzma2
import org.jetbrains.gradle.plugins.seekable
import java.io.InputStream
import java.net.URI

class XZArchiver(private val resource: ReadableResourceInternal) : ReadableResourceInternal by resource {

    private val uri: URI = URIBuilder(resource.uri).schemePrefix("xz:").build()

    override fun getURI() = uri

    override fun read(): InputStream = resource.backingFile.seekable().lzma2()

}