package com.jimmy.sheepcardgame.data

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Deck(val cards: MutableList<Card> = getFullDeck(), val discardPile: MutableList<Card> = mutableListOf()){
    companion object{
        val MaxSize get() = getFullDeckUnshuffled().size
        private var id = 0
        private fun nextId() = id++

        fun getFullDeck(): MutableList<Card> {
            id = Random.nextInt(4785)
            return getFullDeckUnshuffled().shuffled().map {
                when(it){
                    is Card.GoldCard     -> it.copy(id = nextId())
                    is Card.ModifierCard -> it.copy(id = nextId())
                    is Card.SheepCard    -> it.copy(id = nextId())
                    is Card.SpecialCard  -> it.copy(id = nextId())
                }
            }.toMutableList()
        }

        fun getFullDeckUnshuffled(): List<Card> = listOf(
            Card.SheepCard(0, SheepSide.Front, SheepColor.White),
            Card.SheepCard(0, SheepSide.Back, SheepColor.White),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Orange),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Orange),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Magenta),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Magenta),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Cyan),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Cyan),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Beige),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Beige),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Yellow),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Yellow),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Lime),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Lime),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Pink),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Pink),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Grey),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Grey),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Brown),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Brown),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Mint),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Mint),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Purple),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Purple),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Blue),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Blue),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Green),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Green),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Red),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Red),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Black),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Black),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Rainbow),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Rainbow),
            Card.SheepCard(0, SheepSide.Front, SheepColor.Rainbow),
            Card.SheepCard(0, SheepSide.Back, SheepColor.Rainbow),
            Card.SpecialCard(0, SpecialType.Wheat),
            Card.SpecialCard(0, SpecialType.Wheat),
            Card.SpecialCard(0, SpecialType.Wheat),
            Card.SpecialCard(0, SpecialType.Wolf),
            Card.SpecialCard(0, SpecialType.Wolf),
            Card.SpecialCard(0, SpecialType.Yoink),
            Card.SpecialCard(0, SpecialType.Yoink),
            Card.SpecialCard(0, SpecialType.ReFlip),
            Card.SpecialCard(0, SpecialType.ReFlip),
            Card.ModifierCard(0, ModifierType.Paint),
            Card.ModifierCard(0, ModifierType.Paint),
            Card.ModifierCard(0, ModifierType.Franken),
            Card.ModifierCard(0, ModifierType.Franken),
            Card.GoldCard(0, GoldCardType.Remove),
            Card.GoldCard(0, GoldCardType.Yoink),
            Card.GoldCard(0, GoldCardType.Lure),
            Card.GoldCard(0, GoldCardType.Halve),
            Card.GoldCard(0, GoldCardType.Recover)
        )
    }
}
