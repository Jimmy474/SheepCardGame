package com.jimmy.sheepcardgame

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    @Test
    fun `rooms endpoint starts empty`() = testApplication {
        RoomManager.rooms.clear()
        application {
            gameServerModule()
        }
        val response = client.get("/rooms")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }
}
