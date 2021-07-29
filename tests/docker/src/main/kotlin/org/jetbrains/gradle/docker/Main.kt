package org.jetbrains.gradle.docker

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
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
