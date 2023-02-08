package org.jetbrains.gradle.awslambdaruntime

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

suspend inline fun <reified Input, reified Output> handleRequest(client: HttpClient, function: (Input, AWSContext) -> Output) {
    val (response, awsContext) = try {
        val r = client.get("http://$AWS_LAMBDA_RUNTIME_API/2018-06-01/runtime/invocation/next")
        r to r.getAWSHeaders()
    } catch (ex: Throwable) {
        client.post("http://$AWS_LAMBDA_RUNTIME_API/2018-06-01/runtime/init/error") {
            setError(ex)
        }
        // also log
        return
    }

    val output = try {
        function(response.body(), response.getAWSHeaders())
    } catch (ex: Throwable) {
        client.post("http://$AWS_LAMBDA_RUNTIME_API/2018-06-01/runtime/invocation/${awsContext.awsRequestId}/error") {
            setError(ex)
        }
        // also log
        return
    }

    client.post("http://$AWS_LAMBDA_RUNTIME_API/2018-06-01/runtime/invocation/${awsContext.awsRequestId}/response") {
        setBody(output)
    }
}