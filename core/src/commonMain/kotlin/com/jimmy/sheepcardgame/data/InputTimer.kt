package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
data class InputTimer(
    val deadline: Long,
    val playerId: Long? = null,
    val kind: Kind
) {
    @Serializable
    enum class Kind { Turn, CoinFlip, Selection }
}