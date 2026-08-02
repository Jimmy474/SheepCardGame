package com.jimmy.sheepcardgame

interface Connection {
    val id: Long
    val name: String
    fun sendEvent(event: S2CEvent)
}