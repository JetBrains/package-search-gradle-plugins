package org.jetbrains.gradle.awslambdaruntime

import com.github.lamba92.aws.lambda.runtime.handleRequest
import kotlinx.serialization.Serializable

suspend fun main() = handleRequest { input: HelloWorld, _ -> input.hello }

@Serializable
data class HelloWorld(val hello: String)