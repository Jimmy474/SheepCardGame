package com.jimmy.sheepcardgame

import io.ktor.websocket.*
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

@OptIn(ExperimentalAtomicApi::class)
class Connection(val session: DefaultWebSocketSession, val name: String) {
    val id: Long = idCounter.fetchAndIncrement()

    companion object {
        val idCounter = AtomicLong(0)
    }
}
