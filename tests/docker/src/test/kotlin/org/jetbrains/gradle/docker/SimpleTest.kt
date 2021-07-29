package org.jetbrains.gradle.docker

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleTest {

    @Test
    fun testGetEmptyRoute()  {
        withTestApplication(Application::simpleModule) {
            handleRequest(HttpMethod.Get, "").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Hello World!", response.content)
            }
        }
    }
}
