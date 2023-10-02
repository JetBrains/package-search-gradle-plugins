package org.jetbrains.gradle.upx.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
        followRedirects = true
    }
    val json = Json { prettyPrint = true }
    val ip = client.get("https://api.ipify.org?format=json")
        .body<IpResponse>()
        .ip
    val response = client.get("https://www.timeapi.io/api/Time/current/ip?ipAddress=$ip")
        .body<TimeApiResponse>()

    println(json.encodeToString(response))
}

@Serializable
data class IpResponse(val ip: String)

@Serializable
data class TimeApiResponse(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val seconds: Int,
    val milliSeconds: Int,
    val dateTime: String,
    val date: String,
    val time: String,
    val timeZone: String,
    val dayOfWeek: String,
    val dstActive: Boolean
)