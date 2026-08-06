@file:OptIn(ExperimentalWasmJsInterop::class)

package com.jimmy.sheepcardgame

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.await
import kotlin.js.Promise

actual fun httpClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient(Js) {
    config(this)

    engine {

    }
}
@JsModule("@discord/embedded-app-sdk")
external object DiscordModule {
    @JsName("DiscordSDK")
    class DiscordSDK(clientId: String) : JsAny {
        fun ready(): Promise<JsAny?>
        val commands: DiscordCommands
        val channelId: String?
        val guildId: String?
    }
}

external interface DiscordCommands : JsAny {
    fun authorize(options: JsAny): Promise<DiscordAuthorizeResult>
    fun authenticate(options: JsAny): Promise<DiscordAuthResult>
}

external interface DiscordAuthorizeResult : JsAny {
    val code: String
}

external interface DiscordAuthResult : JsAny {
    val user: DiscordUser
}

external interface DiscordUser : JsAny {
    val id: String
    val username: String
    val global_name: String?
    val avatar: String?
}

private fun createAuthorizeOptions(): JsAny = js("({ client_id: '1534122316597039134', response_type: 'code', state: '', prompt: 'none', scope: ['identify'] })")
private fun createAuthOptions(accessToken : String): JsAny = js("({ access_token: 'accessToken' })")

object DiscordBridge {
    private var sdkInstance: DiscordModule.DiscordSDK? = null

    suspend fun initializeAndAuthorize(): Boolean {
        val sdk = DiscordModule.DiscordSDK("1534122316597039134")
        sdkInstance = sdk
        sdk.ready().await()
        return true
    }

    suspend fun getCurrentUser(): UserProfile {
        val sdk = sdkInstance ?: throw IllegalStateException("SDK not initialized")

        val authorizeOptions = createAuthorizeOptions()

        val authorizeResult = sdk.commands.authorize(authorizeOptions).await()
        val code = authorizeResult.code

        val accessToken = fetchTokenFromMyBackend(code)

        val authOptions = createAuthOptions(accessToken)
        val authResult = sdk.commands.authenticate(authOptions).await()

        val discordUser = authResult.user
        val currentChannelId = sdk.channelId
        val currentGuildId = sdk.guildId
        val activityIsDM = (currentGuildId == null)

        val avatarUrl = if (discordUser.avatar != null) {
            "https://cdn.discordapp.com/avatars/${discordUser.id}/${discordUser.avatar}.png?size=256"
        } else {
            val index = (discordUser.id.toLong() shr 22) % 6
            "https://cdn.discordapp.com/embed/avatars/$index.png"
        }

        return UserProfile(
            id = discordUser.id,
            name = discordUser.global_name ?: discordUser.username,
            avatarUrl = avatarUrl,
            channelId = currentChannelId,
            isDM = activityIsDM
        )
    }
}

suspend fun fetchTokenFromMyBackend(code: String): String {
    return HttpClient(Js).post(GameClient.getUrl("token")) {
        setBody("""{"code": "$code"}""")
    }.bodyAsText()
}