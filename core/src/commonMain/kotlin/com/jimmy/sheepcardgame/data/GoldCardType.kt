package com.jimmy.sheepcardgame.data

enum class GoldCardType(val title: String, val amount: String, val location: String, val description: String) {
    Remove("Remove", "2 SHEEP", "to the discard pile", "Remove 2 Sheep from opponent of your choice"),
    Yoink("Yoink", "ENTIRE HAND", "to your hand", "Yoink entire hand of opponent of your choice"),
    Lure("Lure", "2 SHEEP", "to your field", "Lure 2 Sheep from opponent of your choice to your flock"),
    Halve("Halve", "2 SHEEP", "to your hand", "Halve 2 Sheep from the flock of opponent of your choice and put them in your hand, Remaining 2 halves return to opponent's hand"),
    Recover("Recover", "1 SHEEP", "to your field", "Recover 1 Sheep from the discard pile")
}