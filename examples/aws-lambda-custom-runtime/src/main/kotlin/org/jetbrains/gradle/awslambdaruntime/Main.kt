package org.jetbrains.gradle.awslambdaruntime

import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.api.client.AWSLambda
import kotlin.reflect.KClass

fun main() {
    AWSLambda.main(arrayOf(ExampleHandler::class.awsIdentifier()))
}

fun <T, R> KClass<out RequestHandler<T, R>>.awsIdentifier() =
    "${qualifiedName}::handleRequest"