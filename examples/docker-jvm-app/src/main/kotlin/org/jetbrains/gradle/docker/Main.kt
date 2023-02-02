package org.jetbrains.gradle.docker

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope

suspend fun main(): Unit = coroutineScope {
    startServer(CIO, port = 8080) {
        simpleModule()
    }
}

fun Application.simpleModule() {
    routing {
        get {
            call.respondText("Hello World!")
        }
    }
}
