package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
data class Flock(
    val sheep: List<Sheep> = emptyList(),
){
    val isWheatProtected get() = sheep.any { it.isFrankenButts }
    val isWolfProtected get() = sheep.any { it.isFrankenHeads }
}

