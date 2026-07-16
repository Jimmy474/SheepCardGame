package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.data.ClientRoom
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.get
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive

class GameClient(
    val eventHandler: (S2CEvent) -> Unit,
) {

    companion object {
        private const val HOST = "127.0.0.1"
        private const val PORT = 8080
    }

    private val client = httpClient {

        defaultRequest {
            url.host = HOST
            url.port = PORT
        }

        install(WebSockets)

        install(ContentNegotiation) {
            json()
        }
    }

    private var webSocketSession: DefaultClientWebSocketSession? = null

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    suspend fun connect(
        playerName: String,
        action: String,
        successCallback: () -> Unit,
        failedCallback: () -> Unit,
        code: String? = null,
    ) {
        try {
            _connectionStatus.value = "Connecting..."

            client.webSocket(method = HttpMethod.Get, host = HOST, port = PORT, path = "/play", request = {
                url {
                    parameters.append("playerName", playerName)
                    parameters.append("action", action)
                    if (code != null) parameters.append("roomCode", code)
                }
            }) {
                webSocketSession = this
                _connectionStatus.value = "Connected"
                successCallback()

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        handleIncomingMessage(frame.readText())
                    }
                }
            }
        } catch (e: Exception) {
            _connectionStatus.value = "Error: ${e.message}"
            failedCallback()
        } finally {
            _connectionStatus.value = "Disconnected"
            webSocketSession = null
        }
    }

    suspend fun getRoomsList(): List<ClientRoom> {
        return client.get {
            url {
                path("rooms")
            }
        }.body<List<ClientRoom>>()
    }

    suspend fun disconnect() {
        webSocketSession?.close(CloseReason(CloseReason.Codes.NORMAL, "Player left"))
        webSocketSession = null
    }

    suspend fun sendEventToServer(event: C2SEvent) {
        try {
            webSocketSession?.send(Frame.Text(event.encodeToString()))
        } catch (e: Exception) {
            println("Failed to send message: ${e.message}")
        }
    }

    private fun handleIncomingMessage(jsonText: String) {
        try {
            val event = S2CEvent.decodeFromString(jsonText)
            eventHandler(event)
        } catch (_: Exception) {
            println("Failed to parse incoming message: $jsonText")
        }
    }
}