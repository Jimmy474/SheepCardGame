package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.data.ClientRoom
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.cbor.cbor
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::gameServerModule).start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.gameServerModule() {

    install(ContentNegotiation) {
        cbor()
    }


    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
    }

    routing {

        get("/rooms") {
            val availableRooms: List<ClientRoom> = RoomManager.rooms.values.filter { !it.isFull() && !it.isStarted }.shuffled().take(20).map { it.asClientRoom() }
            call.respond(availableRooms)
        }

        webSocket("/play") {
            val params = call.request.queryParameters
            val name = params["playerName"]
            val action = params["action"]
            val roomCode = params["roomCode"]

            if (name == null || action == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Player name and action required"))
                return@webSocket
            }

            val thisServerConnection = ServerConnection(this, name)
            val room: Room

            when (action) {
                "create" -> {
                    room = RoomManager.newRoom(thisServerConnection)
                    log.info("Server: $name created room ${room.code}")
                }

                "join"   -> {
                    if (roomCode == null) {
                        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Room code required to join"))
                        return@webSocket
                    }

                    val existingRoom = RoomManager.rooms[roomCode.uppercase()]
                    if (existingRoom == null) {
                        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room not found"))
                        return@webSocket
                    }

                    val didJoin = existingRoom.join(thisServerConnection)
                    if (didJoin) {
                        room = existingRoom
                        log.info("Server: $name joined room ${room.code}")
                    } else {
                        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room is full"))
                        return@webSocket
                    }
                }

                else     -> {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid action"))
                    return@webSocket
                }
            }

            try {
                while (true) {
                    val event = receiveDeserialized<C2SEvent>()
                    room.handleC2SEvent(thisServerConnection, event)
                }
            } catch (e: Exception) {
                log.error("Server: ${thisServerConnection.name} disconnected unexpectedly: ${e.localizedMessage}")
            } finally {
                room.leave(thisServerConnection.id)
            }
        }
    }
}

object RoomManager {
    val rooms = ConcurrentHashMap<String, Room>()

    fun newRoom(host: ServerConnection): Room {
        val code = generateRoomCode()
        Room(code, host) {
            rooms.remove(it)
        }.let {
            rooms[code] = it
            return it
        }
    }

    private fun generateRoomCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9').asIterable()
        var code: String
        do {
            code = (0..5).map { allowedChars.random() }.joinToString("")
        } while (rooms.containsKey(code))
        return code
    }
}