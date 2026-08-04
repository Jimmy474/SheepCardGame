package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.data.ClientRoom
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.cbor.cbor
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::gameServerModule).start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.gameServerModule() {

    val env = dotenv{
        directory = "/etc/secrets"
        filename = ".env"
    }
    val httpClient = HttpClient(OkHttp) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

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

        post("/token") {
            val request = call.receive<TokenRequest>()
            val clientId = env["DISCORD_CLIENT_ID"] ?: throw Exception("DISCORD_CLIENT_ID not set")
            val clientSecret = env["DISCORD_CLIENT_SECRET"] ?: throw Exception("DISCORD_CLIENT_SECRET not set")

            val tokenResponse = httpClient.post("https://discord.com/api/v10/oauth2/token") {
                setBody(FormDataContent(Parameters.build {
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("grant_type", "authorization_code")
                    append("code", request.code)
                }))
            }.body<DiscordTokenResponse>()

            val userProfile = httpClient.get("https://discord.com/api/v10/users/@me") {
                header(HttpHeaders.Authorization, "Bearer ${tokenResponse.accessToken}")
            }.body<DiscordUser>()

            call.respond(mapOf(
                "username" to (userProfile.globalName ?: userProfile.username),
                "id" to userProfile.id
            ))
        }

        get("/rooms") {
            val availableRooms: List<ClientRoom> = RoomManager.rooms.values.filter { !it.isFull() && !it.isStarted }.shuffled().take(20).map { it.asClientRoom() }
            call.respond(availableRooms)
        }

        webSocket("/play") {
            val params = call.request.queryParameters
            val name = params["playerName"]
            val roomCode = params["roomCode"]

            if (name == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Player name is required"))
                return@webSocket
            }

            val thisServerConnection = ServerConnection(this, name)
            val room: Room = RoomManager[thisServerConnection, roomCode]
            if(room.host.id == thisServerConnection.id && !room.join(thisServerConnection)) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room is full"))
                return@webSocket
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

@Serializable
data class TokenRequest(val code: String)

@Serializable
data class DiscordTokenResponse(@SerialName("access_token") val accessToken: String)

@Serializable
data class DiscordUser(
    val id: String,
    val username: String,
    @SerialName("global_name") val globalName: String? = null,
    val avatar: String? = null
)

object RoomManager {
    val rooms = ConcurrentHashMap<String, Room>()

    operator fun get(host: ServerConnection, roomCode: String?): Room {
        val code = generateRoomCode()
        return rooms.getOrPut(code) { Room(code, host) { rooms.remove(it) } }
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