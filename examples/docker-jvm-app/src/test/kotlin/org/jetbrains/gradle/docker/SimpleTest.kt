package org.jetbrains.gradle.docker

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleTest {

    @Test
    fun testGetEmptyRoute() {
        testApplication {
            application {
                simpleModule()
            }

            val response = client.get("")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Hello World!", response.bodyAsText())
        }
    }
}
