package org.jetbrains.gradle.awslambdaruntime

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.toDuration

var HttpTimeout.HttpTimeoutCapabilityConfiguration.requestTimeout: kotlin.time.Duration?
    get() = requestTimeoutMillis?.toDuration(DurationUnit.MILLISECONDS)
    set(value) {
        requestTimeoutMillis = value?.inWholeMilliseconds
    }

val AWS_LAMBDA_RUNTIME_API: String by System.getenv()

@Serializable
data class ErrorRequest(
    val errorMessage: String,
    val errorType: String,
    val stackStrace: List<String>
)

fun HttpRequestBuilder.setError(ex: Throwable) {
    header(HttpHeaders.`Lambda-Runtime-Function-Error-Type`, "Runtime.${ex::class.simpleName}")
    setBody(ErrorRequest(ex.message ?: "", ex::class.simpleName!!, ex.stackTraceToString().lines()))
}

@Serializable
data class HelloWorld(val hello: String)

fun HttpResponse.getAWSHeaders() = AWSContext(
    headers[HttpHeaders.`Lambda-Runtime-Aws-Request-Id`]!!,
    headers[HttpHeaders.`Lambda-Runtime-Deadline-Ms`]!!.toLong(),
    headers[HttpHeaders.`Lambda-Runtime-Invoked-Function-Arn`]!!,
    headers[HttpHeaders.`Lambda-Runtime-Trace-Id`]
)

data class AWSContext(
    val awsRequestId: String,
    val runtimeDeadlineMs: Long,
    val invokedFunctionArn: String,
    val traceId: String?
)

