package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
sealed interface GameEvents {
    val id: Int

    @Serializable
    data class TurnChange(override val id: Int, val player: String) : GameEvents

    @Serializable
    data class DrawCards(override val id: Int, val cardsAmount: Int, val player: String) : GameEvents

    @Serializable
    data class PlaceSheep(override val id: Int, val sheep: String, val player: String) : GameEvents

    @Serializable
    data class FixSheep(override val id: Int, val fixSheepType: FixSheepType, val sheep: String, val player: String, val opponent: String?) : GameEvents

    @Serializable
    data class YoinkCards(override val id: Int, val cardsAmount: Int, val player: String, val opponent: String) : GameEvents

    @Serializable
    data class WheatWolf(override val id: Int, val isWheat: Boolean, val sheep: String, val player: String, val opponent: String) : GameEvents

    @Serializable
    data class PlayGoldCard(override val id: Int, val goldCardType: GoldCardType, val player: String, val opponent: String) : GameEvents

    @Serializable
    data class CoinFlipResult(override val id: Int, val attacker: String, val target: String, val playerChoice: Boolean, val result: Boolean) : GameEvents

    @Serializable
    data class ReFlipped(override val id: Int, val player: String) : GameEvents

    @Serializable
    data class GoldCardResult(override val id: Int, val goldCardType: GoldCardType, val amount: Int, val player: String, val opponent: String) : GameEvents

    @Serializable
    data class DiscardedCards(override val id: Int, val cardsAmount: Int, val player: String) : GameEvents
}
