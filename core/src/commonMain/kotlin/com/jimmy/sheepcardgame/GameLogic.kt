package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.data.Card
import com.jimmy.sheepcardgame.data.ModifierType
import com.jimmy.sheepcardgame.data.Sheep
import com.jimmy.sheepcardgame.data.SheepColor
import com.jimmy.sheepcardgame.data.SheepSide

object GameLogic {

    fun isValidSheep(cards: List<Card>): Boolean {

        if (cards.size !in 2..3) return false

        val heads = cards.filterIsInstance<Card.SheepCard>().filter { it.sheepSide == SheepSide.Front }
        val butts = cards.filterIsInstance<Card.SheepCard>().filter { it.sheepSide == SheepSide.Back }
        if (heads.size == 1 && butts.size == 1) {
            return (heads.first().sheepColor == butts.first().sheepColor && cards.size == 2)
                    || ((heads.first().sheepColor == SheepColor.Rainbow || butts.first().sheepColor == SheepColor.Rainbow) && cards.size == 2)
                    || (cards.size == 3 && cards.any { it is Card.ModifierCard && it.modifierType == ModifierType.Paint })
        }

        return ((heads.size == 2 && butts.isEmpty()) || (heads.isEmpty() && butts.size == 2)) && cards.size == 3 && cards.any { it is Card.ModifierCard && it.modifierType == ModifierType.Franken }
    }

    fun buildSheep(cards: List<Card>): Sheep? {
        if (!isValidSheep(cards)) return null
        val modifierCard = cards.filterIsInstance<Card.ModifierCard>().firstOrNull()
        return if (modifierCard == null) {
            Sheep(
                cards.filterIsInstance<Card.SheepCard>().firstOrNull { it.sheepSide == SheepSide.Front } ?: return null,
                cards.filterIsInstance<Card.SheepCard>().firstOrNull { it.sheepSide == SheepSide.Back } ?: return null
            )
        } else {
            when (modifierCard.modifierType) {
                ModifierType.Paint   -> Sheep(
                    cards.filterIsInstance<Card.SheepCard>().firstOrNull { it.sheepSide == SheepSide.Front } ?: return null,
                    cards.filterIsInstance<Card.SheepCard>().firstOrNull { it.sheepSide == SheepSide.Back } ?: return null,
                    modifierCard
                )

                ModifierType.Franken -> Sheep(
                    cards.filterIsInstance<Card.SheepCard>().first(),
                    cards.filterIsInstance<Card.SheepCard>().last(),
                    modifierCard
                )
            }
        }
    }

    fun deFrankenReplacement(sheep: Sheep, card: Card.SheepCard): SheepSide? {
        return when {
            sheep.isFrankenHeads && card.sheepSide == SheepSide.Back && card.sheepColor == sheep.head.sheepColor  -> SheepSide.Back
            sheep.isFrankenHeads && card.sheepSide == SheepSide.Back && card.sheepColor == sheep.butt.sheepColor  -> SheepSide.Front
            sheep.isFrankenButts && card.sheepSide == SheepSide.Front && card.sheepColor == sheep.head.sheepColor -> SheepSide.Back
            sheep.isFrankenButts && card.sheepSide == SheepSide.Front && card.sheepColor == sheep.butt.sheepColor -> SheepSide.Front
            else                                                                                                  -> null
        }
    }

    fun dePaintReplacement(sheep: Sheep, card: Card.SheepCard): SheepSide? {
        if (!sheep.isPaint) return null

        return when (card.sheepSide) {
            SheepSide.Back if card.sheepColor == sheep.head.sheepColor  -> SheepSide.Back
            SheepSide.Front if card.sheepColor == sheep.butt.sheepColor -> SheepSide.Front
            else                                                        -> null
        }
    }

    fun deRainbowReplacement(sheep: Sheep, card: Card.SheepCard): SheepSide? {
        return if (sheep.isRainbowHead && card.sheepSide == SheepSide.Front && card.sheepColor == sheep.butt.sheepColor) SheepSide.Front
        else if (sheep.isRainbowHead && card.sheepSide == SheepSide.Back && card.sheepColor == SheepColor.Rainbow) SheepSide.Back
        else if (sheep.isRainbowButt && card.sheepSide == SheepSide.Front && card.sheepColor == SheepColor.Rainbow) SheepSide.Front
        else if (sheep.isRainbowButt && card.sheepSide == SheepSide.Back && card.sheepColor == sheep.head.sheepColor) SheepSide.Back
        else null
    }

    fun getSheep(cards: List<Card>): List<Sheep>{
        if(cards.size < 2) return emptyList()
        return getUniqueCombinationsOfThree(cards.filter{ it is Card.SheepCard || it is Card.ModifierCard }).mapNotNull { buildSheep(it) }
    }

    fun <T> getUniqueCombinationsOfThree(list: List<T>): List<List<T>> {
        val uniqueList = list.distinct()
        if(uniqueList.size < 3) return listOf(uniqueList)

        val result = mutableListOf<List<T>>()
        val size = uniqueList.size

        for (i in 0 until size - 2) {
            for (j in i + 1 until size - 1) {
                for (k in j + 1 until size) {
                    result.add(listOf(uniqueList[i], uniqueList[j], uniqueList[k]))
                }
            }
        }
        return result
    }

}