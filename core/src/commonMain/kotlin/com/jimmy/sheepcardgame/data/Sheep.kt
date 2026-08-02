package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable

@Serializable
data class Sheep(
    val head: Card.SheepCard,
    val butt: Card.SheepCard,
    val modifier: Card.ModifierCard? = null,
    val id: Int = ID_COUNTER,
) {

    companion object {
        var ID_COUNTER = 0
            get() = field++
    }

    val name: String
        get() = when {
            isFrankenHeads -> "${head.sheepColor.name} & ${butt.sheepColor.name} Franken Heads"
            isFrankenButts -> "${head.sheepColor.name} & ${butt.sheepColor.name} Franken Butts"
            isPaint        -> "Painted ${head.sheepColor.name} & ${butt.sheepColor.name}"
            isFullRainbow  -> "Rainbow"
            else           -> "${head.sheepColor.name} & ${butt.sheepColor.name}"
        }

    fun deFrankenCandidates(hand: List<Card>): List<Card.SheepCard> {
        if (modifier?.modifierType != ModifierType.Franken) return emptyList()

        val sheepCards = hand.filterIsInstance<Card.SheepCard>()

        return when {
            isFrankenHeads -> sheepCards.filter {
                (it.sheepColor == head.sheepColor && it.sheepSide == SheepSide.Back) ||
                        (it.sheepColor == butt.sheepColor && it.sheepSide == SheepSide.Back)
            }

            isFrankenButts -> sheepCards.filter {
                (it.sheepColor == head.sheepColor && it.sheepSide == SheepSide.Front) ||
                        (it.sheepColor == butt.sheepColor && it.sheepSide == SheepSide.Front)
            }

            else           -> emptyList()
        }
    }

    fun dePaintCandidates(hand: List<Card>): List<Card.SheepCard> {
        if (modifier?.modifierType != ModifierType.Paint) return emptyList()

        return hand
            .filterIsInstance<Card.SheepCard>()
            .filter {
                (it.sheepColor == head.sheepColor && it.sheepSide == SheepSide.Back) ||
                        (it.sheepColor == butt.sheepColor && it.sheepSide == SheepSide.Front)
            }
    }

    fun deRainbowCandidate(hand: List<Card>): List<Card.SheepCard> {
        if (isFranken || isPaint || isFullRainbow) return emptyList()
        return hand.filterIsInstance<Card.SheepCard>().filter {
            if (isRainbowHead) (it.sheepSide == SheepSide.Front && it.sheepColor == butt.sheepColor) || (it.sheepSide == SheepSide.Back && it.sheepColor == SheepColor.Rainbow)
            else if (isRainbowButt) (it.sheepSide == SheepSide.Back && it.sheepColor == head.sheepColor) || (it.sheepSide == SheepSide.Front && it.sheepColor == SheepColor.Rainbow)
            else false
        }
    }

    val isRainbowHead get() = head.sheepColor == SheepColor.Rainbow
    val isRainbowButt get() = butt.sheepColor == SheepColor.Rainbow

    val isFullRainbow get() = isRainbowHead && isRainbowButt

    val isFranken get() = modifier?.modifierType == ModifierType.Franken
    val isPaint get() = modifier?.modifierType == ModifierType.Paint
    val isFrankenHeads get() = head.sheepSide == SheepSide.Front && butt.sheepSide == SheepSide.Front && isFranken
    val isFrankenButts get() = head.sheepSide == SheepSide.Back && butt.sheepSide == SheepSide.Back && isFranken
    val cards
        get() = buildList {
            add(head)
            add(butt)
            modifier?.let { add(it) }
        }
}

