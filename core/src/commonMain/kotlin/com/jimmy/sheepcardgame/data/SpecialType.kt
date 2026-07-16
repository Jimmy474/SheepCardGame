package com.jimmy.sheepcardgame.data

enum class SpecialType(val title: String, val description: String) {
    Wheat("Wheat", "LURE a SHEEP to your field"),
    Wolf("Wolf", "REMOVE a SHEEP to the discard pile"),
    Yoink("Yoink!", "TAKE 2 CARDS from someone's hand"),
    ReFlip("Re-Flip", "FLIP THE COIN AGAIN!")
}