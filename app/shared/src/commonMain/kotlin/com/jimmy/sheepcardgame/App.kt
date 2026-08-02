package com.jimmy.sheepcardgame

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.jimmy.sheepcardgame.ui.navigation.Routes
import com.jimmy.sheepcardgame.ui.navigation.RoutesConfig
import com.jimmy.sheepcardgame.ui.screens.*
import com.jimmy.sheepcardgame.ui.theme.CardGameTheme

@Composable
fun App() {
    val backStack = rememberNavBackStack(RoutesConfig, Routes.HomeRoute)

    fun navigate(route: Routes) {
        backStack.add(route)
    }

    CardGameTheme {
        Surface {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                onBack = {
                    if (backStack.lastOrNull() !is Routes.GameRoute) backStack.removeLastOrNull()
                },
                entryProvider = entryProvider {
                    entry<Routes.HomeRoute> {
                        HomeScreen(::navigate)
                    }
                    entry<Routes.RoomRoute> {
                        RoomScreen(::navigate)
                    }
                    entry<Routes.HowToPlayRoute> {
                        HowToPlayScreen(it, ::navigate)
                    }
                    entry<Routes.SettingsRoute> {
                        SettingsScreen(it, ::navigate)
                    }
                    entry<Routes.AboutRoute> {
                        AboutScreen(it, ::navigate)
                    }
                    entry<Routes.GameRoute> {
                        GameScreen{
                            backStack.removeLastOrNull()
                        }
                    }
                    entry<Routes.LocalGameRoute> {
                        LocalGameScreen{
                            backStack.removeLastOrNull()
                        }
                    }
                }
            )
        }
    }
}