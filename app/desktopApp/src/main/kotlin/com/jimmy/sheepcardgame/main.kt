package com.jimmy.sheepcardgame

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jimmy.sheepcardgame.ui.CardDisplayPreview

fun main() = application {

    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "SheepCardGame",
    ) {
        App()
    }
}