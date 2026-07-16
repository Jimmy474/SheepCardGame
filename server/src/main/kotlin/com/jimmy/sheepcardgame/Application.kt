package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.data.ClientRoom
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::gameServerModule).start(wait = true)
}

fun Application.gameServerModule() {

    install(ContentNegotiation) {
        json()
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
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
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

            val thisConnection = Connection(this, name)
            val room: Room

            when (action) {
                "create" -> {
                    room = RoomManager.newRoom(thisConnection)
                    log.info("Server: $name created room ${room.code}")
                }

                "join"   -> {
                    if (roomCode == null) {
                        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Room code required to join"))
                        return@webSocket
                    }

                    val existingRoom = RoomManager.rooms[roomCode]
                    if (existingRoom == null) {
                        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room not found"))
                        return@webSocket
                    }

                    val didJoin = existingRoom.join(thisConnection)
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
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    val c2SEvent = C2SEvent.decodeFromString(receivedText)
                    room.handleC2SEvent(thisConnection, c2SEvent)
                }
            } catch (e: Exception) {
                log.error("Server: ${thisConnection.name} disconnected unexpectedly: ${e.localizedMessage}")
            } finally {
                val isEmpty = room.leave(thisConnection)

                if (isEmpty) {
                    RoomManager.rooms.remove(room.code)
                    log.info("Server: Room ${room.code} destroyed (empty).")
                } else if (thisConnection == room.host) {
                    room.changeHost()
                } else {
                    log.info("Server: ${thisConnection.name} left room ${room.code}.")
                }
            }
        }
    }
}
