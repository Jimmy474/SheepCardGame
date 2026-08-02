package com.jimmy.sheepcardgame

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

@OptIn(ExperimentalAtomicApi::class)
class ServerConnection(private val session: DefaultWebSocketServerSession, override val name: String): Connection {
    override val id: Long = idCounter.fetchAndIncrement()
    private val mutex = Mutex()

    override fun sendEvent(event: S2CEvent){
        session.launch {
            mutex.withLock {
                session.sendSerialized(event)
            }
        }
    }

    companion object {
        val idCounter = AtomicLong(0)
    }
}
