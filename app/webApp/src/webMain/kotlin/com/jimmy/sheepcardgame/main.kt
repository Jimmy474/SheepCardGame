package com.jimmy.sheepcardgame

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    val isDiscord = getPlatform().name.endsWith(".discordsays.com")
    if(isDiscord){
        MainScope().launch {
            try {
                val authReady = DiscordBridge.initializeAndAuthorize()

                if (authReady) {
                    val userData = DiscordBridge.getCurrentUser()

                    ComposeViewport {
                        App(userData)
                    }
                }
            } catch (e: Exception) {
                println("Failed to authenticate with Discord: ${e.message}")
            }
        }
    }else{
        ComposeViewport {
            App()
        }
    }
}