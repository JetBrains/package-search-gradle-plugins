package org.jetbrains.gradle.awslambdaruntime

import io.ktor.http.*

val HttpHeaders.`Lambda-Runtime-Aws-Request-Id`
    get() = "Lambda-Runtime-Aws-Request-Id"
val HttpHeaders.`Lambda-Runtime-Deadline-Ms`
    get() = "Lambda-Runtime-Deadline-Ms"
val HttpHeaders.`Lambda-Runtime-Invoked-Function-Arn`
    get() = "Lambda-Runtime-Deadline-Ms"
val HttpHeaders.`Lambda-Runtime-Trace-Id`
    get() = "Lambda-Runtime-Trace-Id"

val HttpHeaders.`Lambda-Runtime-Function-Error-Type`
    get() = "Lambda-Runtime-Function-Error-Type"