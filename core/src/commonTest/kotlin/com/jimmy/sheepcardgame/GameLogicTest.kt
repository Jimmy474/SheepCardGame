package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.data.Card
import com.jimmy.sheepcardgame.data.ModifierType
import com.jimmy.sheepcardgame.data.SheepColor
import com.jimmy.sheepcardgame.data.SheepSide
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameLogicTest {

    @Test
    fun isValidSheepTest() {

        val redHead = Card.SheepCard(SheepSide.Front, SheepColor.Red)
        val blueHead = Card.SheepCard(SheepSide.Front, SheepColor.Blue)
        val rainbowHead = Card.SheepCard(SheepSide.Front, SheepColor.Rainbow)

        val redButt = Card.SheepCard(SheepSide.Back, SheepColor.Red)
        val blueButt = Card.SheepCard(SheepSide.Back, SheepColor.Blue)
        val rainbowButt = Card.SheepCard(SheepSide.Back, SheepColor.Rainbow)

        val paint = Card.ModifierCard(ModifierType.Paint)
        val franken = Card.ModifierCard(ModifierType.Franken)

        // Invalid number of cards
        assertFalse(GameLogic.isValidSheep(emptyList()), "0 cards should be invalid")
        assertFalse(GameLogic.isValidSheep(listOf(redHead)), "1 card should be invalid")
        assertFalse(GameLogic.isValidSheep(listOf(redHead, redButt, paint, franken)), "More than 3 cards should be invalid")

        // One head + one butt (2 cards)
        assertTrue(GameLogic.isValidSheep(listOf(redHead, redButt)), "Same color head and butt should be valid")
        assertFalse(GameLogic.isValidSheep(listOf(redHead, blueButt)), "Different colors without Paint should be invalid")
        assertTrue(GameLogic.isValidSheep(listOf(rainbowHead, redButt)), "Rainbow head should match any butt")
        assertTrue(GameLogic.isValidSheep(listOf(rainbowHead, blueButt)), "Rainbow head should match any butt")
        assertTrue(GameLogic.isValidSheep(listOf(redHead, rainbowButt)), "Rainbow butt should match any head")
        assertTrue(GameLogic.isValidSheep(listOf(blueHead, rainbowButt)), "Rainbow butt should match any head")
        assertTrue(GameLogic.isValidSheep(listOf(rainbowHead, rainbowButt)), "Double rainbow should be valid")

        // Same type without Franken
        assertFalse(GameLogic.isValidSheep(listOf(redHead, redHead)), "Two heads should be invalid")
        assertFalse(GameLogic.isValidSheep(listOf(redHead, blueHead)), "Two different heads should be invalid")
        assertFalse(GameLogic.isValidSheep(listOf(redButt, redButt)), "Two butts should be invalid")
        assertFalse(GameLogic.isValidSheep(listOf(redButt, blueButt)), "Two different butts should be invalid")

        // Paint modifier (requires one head + one butt)
        assertTrue(GameLogic.isValidSheep(listOf(redHead, blueButt, paint)), "Paint should allow different colors")
        assertTrue(GameLogic.isValidSheep(listOf(redHead, redButt, paint)), "Paint should also allow same colors")
        assertTrue(GameLogic.isValidSheep(listOf(rainbowHead, redButt, paint)), "Rainbow head with Paint should be valid")
        assertTrue(GameLogic.isValidSheep(listOf(redHead, rainbowButt, paint)), "Rainbow butt with Paint should be valid")
        assertTrue(GameLogic.isValidSheep(listOf(rainbowHead, rainbowButt, paint)), "Double rainbow with Paint should be valid")

        // Paint wrong structure
        assertFalse(GameLogic.isValidSheep(listOf(redHead, blueHead, paint)), "Paint should not allow two heads")
        assertFalse(GameLogic.isValidSheep(listOf(redButt, blueButt, paint)), "Paint should not allow two butts")

        // Franken modifier (requires two heads OR two butts)
        assertTrue(GameLogic.isValidSheep(listOf(redHead, blueHead, franken)), "Franken should allow two heads")
        assertTrue(GameLogic.isValidSheep(listOf(redHead, redHead, franken)), "Franken should allow same-color heads")
        assertTrue(GameLogic.isValidSheep(listOf(redButt, blueButt, franken)), "Franken should allow two butts")
        assertTrue(GameLogic.isValidSheep(listOf(redButt, redButt, franken)), "Franken should allow same-color butts")

        // Franken wrong structure
        assertFalse(GameLogic.isValidSheep(listOf(redHead, redButt, franken)), "Franken should not allow one head and one butt")

        // Invalid extra cards
        assertFalse(GameLogic.isValidSheep(listOf(redHead, redButt, redButt)), "Extra sheep card should be invalid")
        assertFalse(GameLogic.isValidSheep(listOf(rainbowHead, redButt, redButt)), "Rainbow head with extra sheep should be invalid")
        assertFalse(GameLogic.isValidSheep(listOf(redHead, rainbowButt, rainbowButt)), "Rainbow butt with extra sheep should be invalid")
        assertFalse(GameLogic.isValidSheep(listOf(rainbowHead, rainbowButt, rainbowButt)), "Double rainbow with extra sheep should be invalid")

        // Modifiers alone / missing sheep
        assertFalse(GameLogic.isValidSheep(listOf(redHead, paint)), "Head and Paint only should be invalid")
        assertFalse(GameLogic.isValidSheep(listOf(redButt, paint)), "Butt and Paint only should be invalid")
        assertFalse(GameLogic.isValidSheep(listOf(paint, franken)), "Only modifiers should be invalid")
    }

}