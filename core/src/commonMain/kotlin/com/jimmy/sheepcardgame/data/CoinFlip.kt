package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
data class CoinFlip(
    val goldCard: Card.GoldCard,
    val attacker: Long,
    val target: Long,
    val playerChoice: Boolean?,
    val currentResult: Boolean?,
    val reFlippable: Int,
    val lastReFlippedBy: Long? = null,
    val skippedReFlip: List<Long>,
    val closedDialog: List<Long>,
    val iteration: Int = 0
){
    val winner: Long
        get() = if (currentResult == playerChoice) attacker else target

    val looser: Long
        get() = if (currentResult != playerChoice) attacker else target
}