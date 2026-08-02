package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
data class RoomSettings(
    val initialHandSize: Int = 5,
    val drawOnEachTurn: Int = 1,
    val minHandSize: Int = 3,
    val maxHandSize: Int = 7,
    val goldCardPenalty: Int = 3,
    val rainbowSheepPoints: Int = 2,
)