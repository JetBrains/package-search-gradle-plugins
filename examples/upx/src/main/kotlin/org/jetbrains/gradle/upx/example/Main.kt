package org.jetbrains.gradle.upx.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

suspend fun main() {
    val client = HttpClient(CIO) {
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
    @SerialName("year") val year: Int,
    @SerialName("month") val month: Int,
    @SerialName("day") val day: Int,
    @SerialName("hour") val hour: Int,
    @SerialName("minute") val minute: Int,
    @SerialName("seconds") val seconds: Int,
    @SerialName("milliSeconds") val milliSeconds: Int,
    @SerialName("dateTime") val dateTime: String,
    @SerialName("date") val date: String,
    @SerialName("time") val time: String,
    @SerialName("timeZone") val timeZone: String,
    @SerialName("dayOfWeek") val dayOfWeek: String,
    @SerialName("dstActive") val dstActive: Boolean
)