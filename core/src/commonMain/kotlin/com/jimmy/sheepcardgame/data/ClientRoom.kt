package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
data class ClientRoom(
    val code: String,
    val players: Int,
    val host: PlayerInfo,
    val deck: Int,
    val discardPile: Int,
    val previousGameScores: List<List<Pair<String,Int>>> = emptyList(),
    val isStarted: Boolean = false,
)
