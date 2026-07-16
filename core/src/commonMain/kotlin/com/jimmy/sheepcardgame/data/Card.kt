package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
sealed interface Card {
    val id: Int

    @Serializable
    data class SheepCard(override val id: Int, val sheepSide: SheepSide, val sheepColor: SheepColor): Card{
        val name = "$sheepColor $sheepSide"
    }
    @Serializable
    data class SpecialCard(override val id: Int, val specialType: SpecialType): Card
    @Serializable
    data class ModifierCard(override val id: Int, val modifierType: ModifierType): Card
    @Serializable
    data class GoldCard(override val id: Int, val goldCardType: GoldCardType): Card
}