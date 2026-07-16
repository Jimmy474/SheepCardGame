package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
data class Opponent(
    val info: PlayerInfo,
    val numCards: Int = 0,
)