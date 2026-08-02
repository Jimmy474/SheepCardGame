package com.jimmy.sheepcardgame

class LocalConnection(override val id: Long, override val name: String, val event: (Long,S2CEvent) -> Unit): Connection {
    override fun sendEvent(event: S2CEvent) = event(id,event)
}