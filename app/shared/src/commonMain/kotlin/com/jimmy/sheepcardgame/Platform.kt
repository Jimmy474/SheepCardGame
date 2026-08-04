package com.jimmy.sheepcardgame

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun httpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient

data class DiscordContext(val isDM: Boolean?, val instanceId: String?, val username: String?)

expect suspend fun getDiscordContext(): DiscordContext