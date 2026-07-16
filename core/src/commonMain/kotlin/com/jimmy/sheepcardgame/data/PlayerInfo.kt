package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
data class PlayerInfo (
    val id: Long,
    val name: String,
    val flock: Flock = Flock(),
)

