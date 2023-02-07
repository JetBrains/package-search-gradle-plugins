package org.jetbrains.gradle.awslambdaruntime

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler

class ExampleHandler : RequestHandler<Unit, Unit> {
    override fun handleRequest(input: Unit, context: Context) {
        println("Hello World!")
    }
}

