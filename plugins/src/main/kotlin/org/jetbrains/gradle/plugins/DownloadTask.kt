package org.jetbrains.gradle.plugins

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.inject.Inject


open class DownloadTask @Inject constructor(@Inject val objects: ObjectFactory) : DefaultTask() {

    @get:Input
    val url = objects.property<String>()

    @get:OutputFile
    val outputFile = objects.fileProperty()

    @TaskAction
    fun dowload() {
        val request = HttpRequest.newBuilder()
            .uri(URI(url.get()))
            .GET()
            .build()
        val body = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build()
            .send(request, HttpResponse.BodyHandlers.ofInputStream())
            .body()
        outputFile.get().asFile.outputStream()
            .use { it.write(body.readAllBytes()) }
    }
}