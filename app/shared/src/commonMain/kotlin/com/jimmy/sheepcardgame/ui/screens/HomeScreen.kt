package com.jimmy.sheepcardgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.jimmy.sheepcardgame.GameViewModel
import com.jimmy.sheepcardgame.ui.navigation.Routes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(route: Routes.HomeRoute, navigateTo: (Routes) -> Unit) {

    Scaffold {
        Column(Modifier.padding(it).fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("Is That Sheep\nLooking At Me?", style = MaterialTheme.typography.displayLargeEmphasized, textAlign = TextAlign.Center)

            ElevatedButton({ navigateTo(Routes.RoomRoute) }){ Text("Play") }
            OutlinedButton({ navigateTo(Routes.HowToPlayRoute) }){ Text("How To Play?") }
            OutlinedButton({ navigateTo(Routes.SettingsRoute) }){ Text("Settings") }
            OutlinedButton({ navigateTo(Routes.AboutRoute) }){ Text("About") }
            OutlinedButton({  }){ Text("Exit") }
        }
    }

}