package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.data.Card
import com.jimmy.sheepcardgame.data.Player
import com.jimmy.sheepcardgame.data.PlayerInfo
import io.ktor.websocket.DefaultWebSocketSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement

@OptIn(ExperimentalAtomicApi::class)
class Connection(val session: DefaultWebSocketSession, val name: String){
    val id: Long = idCounter.fetchAndIncrement()

    companion object{
        val idCounter = AtomicLong(0)
    }
}
