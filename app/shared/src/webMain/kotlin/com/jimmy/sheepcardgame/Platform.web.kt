@file:OptIn(ExperimentalWasmJsInterop::class)

package com.jimmy.sheepcardgame

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

actual fun httpClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient(Js) {
    config(this)

    engine {

    }
}

actual suspend fun getDiscordContext(): DiscordContext {
    return DiscordContext(
        isDM = isDiscordDM(),
        instanceId = getDiscordInstanceId(),
        username = getDiscordUsername()
    )
}

@JsFun("() => window.discordEnv ? window.discordEnv.isDM : false")
external fun isDiscordDM(): Boolean?

@JsFun("() => window.discordEnv ? window.discordEnv.instanceId : null")
external fun getDiscordInstanceId(): String?

@JsFun("() => window.discordEnv ? window.discordEnv.username : null")
external fun getDiscordUsername(): String?