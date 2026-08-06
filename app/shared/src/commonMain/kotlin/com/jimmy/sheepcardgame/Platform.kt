package com.jimmy.sheepcardgame

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun httpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient

data class UserProfile(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val channelId: String?,
    val isDM: Boolean
)