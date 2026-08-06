package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.data.ClientRoom
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.cbor.cbor
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor

class GameClient(
    val eventHandler: (S2CEvent) -> Unit,
) {

    companion object {
        private val currentHost = getPlatform().name
        private val IS_LOCAL = currentHost == "localhost" || currentHost == "127.0.0.1"
        private val IS_DISCORD = currentHost.endsWith(".discordsays.com")

        private const val RENDER_HOST = "sheepcardgame.onrender.com"
        private const val LOCAL_HOST = "127.0.0.1"

        private val HOST = when {
            IS_LOCAL -> LOCAL_HOST
            IS_DISCORD -> currentHost
            else -> RENDER_HOST
        }
        private val HTTP_PROTOCOL = if (IS_LOCAL) URLProtocol.HTTP else URLProtocol.HTTPS
        private val WS_PROTOCOL = if (IS_LOCAL) URLProtocol.WS else URLProtocol.WSS
        private val PORT = if (IS_LOCAL) 8080 else null
        private val PATH_PREFIX = if (IS_DISCORD) "api/" else ""

        fun getUrl(path: String) = "https://$HOST:$PORT/$path"
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient {
        expectSuccess = true

        defaultRequest {
            url {
                protocol = HTTP_PROTOCOL
                host = HOST
                PORT?.let { port = it }
            }
        }

        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
        }

        install(ContentNegotiation) {
            cbor()
        }
    }

    private var webSocketSession: DefaultClientWebSocketSession? = null

    private val _connectionStatus = MutableStateFlow("Disconnected")

    suspend fun connect(
        playerName: String,
        successCallback: () -> Unit,
        failedCallback: () -> Unit,
        code: String? = null,
    ) {
        try {
            _connectionStatus.value = "Connecting..."

            client.webSocket(
                path = "${PATH_PREFIX}play",
                request = {
                    url {
                        protocol = WS_PROTOCOL
                        parameters.append("playerName", playerName)
                        if (code != null) parameters.append("roomCode", code)
                    }
                }
            ) {
                webSocketSession = this
                _connectionStatus.value = "Connected"
                successCallback()

                while (true) {
                    val event = receiveDeserialized<S2CEvent>()
                    eventHandler(event)
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
        return client.get("${PATH_PREFIX}rooms").body<List<ClientRoom>>()
    }

    suspend fun disconnect() {
        webSocketSession?.close(CloseReason(CloseReason.Codes.NORMAL, "Player left"))
        webSocketSession = null
    }

    suspend fun sendEventToServer(event: C2SEvent) {
        try {
            webSocketSession?.sendSerialized(event)
        } catch (e: Exception) {
            println("Failed to send message: ${e.message}")
        }
    }
}