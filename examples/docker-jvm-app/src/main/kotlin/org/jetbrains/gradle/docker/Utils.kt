package org.jetbrains.gradle.docker

import io.ktor.application.*
import io.ktor.server.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

fun <TEngine : ApplicationEngine, TConfiguration : ApplicationEngine.Configuration> CoroutineScope.startServer(
    factory: ApplicationEngineFactory<TEngine, TConfiguration>,
    port: Int = 80,
    host: String = "0.0.0.0",
    watchPaths: List<String>? = null,
    parentCoroutineContext: CoroutineContext = coroutineContext,
    configure: TConfiguration.() -> Unit = {},
    module: Application.() -> Unit
): TEngine =
    if (watchPaths != null)
        embeddedServer(
            factory = factory,
            port = port,
            host = host,
            watchPaths = watchPaths,
            parentCoroutineContext = parentCoroutineContext,
            configure = configure,
            module = module
        ).apply { start() }
    else embeddedServer(
        factory = factory,
        port = port,
        host = host,
        parentCoroutineContext = parentCoroutineContext,
        configure = configure,
        module = module
    ).apply { start() }
