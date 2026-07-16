package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val info: PlayerInfo,
    val hand: List<Card> = emptyList(),
){
    fun removeFromHand(card :Card, n: Int = 1): List<Card> = hand.apply{ repeat(n) { minus(card) } }

    fun addToHand(card: Card, n: Int = 1) = hand.apply { repeat(n){ plus(card) } }
    fun asOpponent() = Opponent(info, hand.size)
}