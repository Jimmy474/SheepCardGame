package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
sealed interface Card {
    val id: Int
    val resourceName: String
    val accessibilityName: String

    @Serializable
    data class SheepCard(override val id: Int, val sheepSide: SheepSide, val sheepColor: SheepColor) : Card {
        val name = "$sheepColor $sheepSide"
        override val resourceName get() = "$sheepColor$sheepSide"
        override val accessibilityName: String get() = "Sheep Card Of Color $sheepColor and Side $sheepSide"
    }

    @Serializable
    data class ModifierCard(override val id: Int, val modifierType: ModifierType) : Card {
        override val resourceName get() = modifierType.name
        override val accessibilityName get() = "Modifier Card Of Type $modifierType"
    }

    @Serializable
    data class SpecialCard(override val id: Int, val specialType: SpecialType) : Card {
        override val resourceName get() = specialType.name
        override val accessibilityName get() = "Special Card Of Type $specialType"
    }

    @Serializable
    data class GoldCard(override val id: Int, val goldCardType: GoldCardType) : Card {
        override val resourceName get() = "Gold${goldCardType.name}"
        override val accessibilityName get() = "Gold Card Of Type $goldCardType"
    }
}

fun Card.rank() = when (this) {
    is Card.SheepCard    -> 1
    is Card.ModifierCard -> 2
    is Card.SpecialCard  -> 3
    is Card.GoldCard     -> 4
}
