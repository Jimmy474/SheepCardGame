package com.jimmy.sheepcardgame.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jimmy.sheepcardgame.GameScreenEvents
import com.jimmy.sheepcardgame.LocalGameViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LocalGameScreen(exit: () -> Unit) {
    val viewModel = koinViewModel<LocalGameViewModel>()
    val state = viewModel.activeState
    var names by remember { mutableStateOf(listOf("Player 1", "Player 2")) }
    val curtain by viewModel.curtain.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { viewModel.onEvent(GameScreenEvents.Leave) }
    }

    if (state == null) {
        LocalGameSetup(names, { names = it }, { viewModel.initialize(names) }, { viewModel.onEvent(GameScreenEvents.Leave); exit() })
        return
    }

    CompositionLocalProvider(LocalOnEvent provides viewModel::onEvent) {
        state.let { ds ->
            GameDragDropOverlay {
                if (state.clientRoom?.isStarted != true)
                    PreStartUI(state, { viewModel.startGame() }, { viewModel.onEvent(GameScreenEvents.Leave); exit() })
                else
                    GameBoardWrapper(ds)
                val dialogs = viewModel.dialogs[ds.player?.info?.id ?: -1L].orEmpty()
                DisplayDialogs(ds, dialogs) { viewModel.onEvent(GameScreenEvents.Leave); exit() }
            }
            AnimatedVisibility(curtain.visible, Modifier, fadeIn(tween(if(curtain.skipEntry) 0 else 200)), fadeOut(tween(200))) {
                PassDeviceScreen(curtain.name)
            }
        }
    }
}

@Composable
private fun PassDeviceScreen(playerName: String) {
    Dialog({}, properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false, usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                Alignment.CenterHorizontally,
            ) {
                Text("Pass the device to", style = MaterialTheme.typography.headlineMedium)
                Text(playerName, style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}

@Composable
private fun LocalGameSetup(
    names: List<String>,
    onNamesChanged: (List<String>) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        Alignment.CenterHorizontally,
    ) {
        Text("Local Game", style = MaterialTheme.typography.displaySmall)
        Text("Choose 2 to 4 players and enter their names.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                enabled = names.size > 2,
                onClick = { onNamesChanged(names.dropLast(1)) },
            ) { Text("-") }
            Text("${names.size} players", style = MaterialTheme.typography.titleLarge)
            Button(
                enabled = names.size < 4,
                onClick = { onNamesChanged(names + "Player ${names.size + 1}") },
            ) { Text("+") }
        }
        names.forEachIndexed { index, name ->
            OutlinedTextField(
                value = name,
                onValueChange = { value ->
                    onNamesChanged(names.toMutableList().also { it[index] = value })
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                singleLine = true,
                label = { Text("Player ${index + 1} name") },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStart, enabled = names.all { it.isNotBlank() }) { Text("START GAME") }
            Button(onClick = onExit) { Text("EXIT") }
        }
    }
}