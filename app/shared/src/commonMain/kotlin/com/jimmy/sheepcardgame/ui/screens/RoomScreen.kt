package com.jimmy.sheepcardgame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jimmy.sheepcardgame.GameState
import com.jimmy.sheepcardgame.GameViewModel
import com.jimmy.sheepcardgame.data.ClientRoom
import com.jimmy.sheepcardgame.data.PlayerInfo
import com.jimmy.sheepcardgame.ui.icons.RefreshIcon
import com.jimmy.sheepcardgame.ui.navigation.Routes
import com.jimmy.sheepcardgame.ui.theme.CardGameTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RoomScreen(navigateTo: (Routes) -> Unit) {

    val viewModel = koinViewModel<GameViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchRoomsList()
    }

    RoomScreenLayout(
        state = state,
        onCreateRoom = {
            viewModel.connectToServerCreateRoom(it, { navigateTo(Routes.GameRoute) }, {})
        },
        onJoinRoom = { name, code ->
            viewModel.connectToServerJoinRoom(name, code, { navigateTo(Routes.GameRoute) }, {})
        },
        fetchRoomsList = { viewModel.fetchRoomsList() }
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFlexBoxApi::class)
private fun RoomScreenLayout(
    state: GameState,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (String, String) -> Unit,
    fetchRoomsList: () -> Unit,
) {
    val name = rememberTextFieldState()
    val code = rememberTextFieldState()
    var isError by remember { mutableStateOf(false) }
    var isErrorCode by remember { mutableStateOf(false) }

    Scaffold { paddingValues ->
        Column(Modifier.padding(paddingValues).fillMaxSize().padding(8.dp), Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally) {
            ElevatedCard {
                Row(Modifier.fillMaxWidth().padding(8.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                    OutlinedTextField(
                        name,
                        label = { Text("Name") },
                        placeholder = { Text("Enter your name") },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        isError = isError,
                        supportingText = if (isError) {
                            {
                                Text("Name must be not empty or blank.")
                            }
                        } else null,
                        inputTransformation = {
                            isError = false
                        }
                    )
                    OutlinedTextField(
                        code,
                        label = { Text("Room Code") },
                        placeholder = { Text("Enter room code") },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        isError = isErrorCode,
                        supportingText = if (isErrorCode) {
                            {
                                Text("Code must be not empty or blank.")
                            }
                        } else null,
                        inputTransformation = {
                            isErrorCode = false
                        }
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally){
                        Button({
                            if (name.text.isBlank()) {
                                isError = true
                                return@Button
                            }
                            onCreateRoom(name.text.toString())
                        }) { Text("Create Room") }
                        Button({
                            if (name.text.isBlank()) {
                                isError = true
                                return@Button
                            }
                            if (code.text.isBlank()) {
                                isErrorCode = true
                                return@Button
                            }
                            onJoinRoom(name.text.toString(), code.text.toString())
                        }) { Text("Join Room") }
                    }
                }
            }

            Card(Modifier.weight(1f)) {
                Row {
                    IconButton({
                        fetchRoomsList()
                    }) {
                        Icon(RefreshIcon, contentDescription = "Refresh Rooms List")
                    }
                }
                FlexBox(Modifier.fillMaxSize().padding(8.dp), {
                    gap(8.dp)
                }) {
                    state.roomsToJoin.forEach {
                        ListItem(
                            onClick = {
                                if (name.text.isBlank()) {
                                    isError = true
                                    return@ListItem
                                }
                                onJoinRoom(name.text.toString(), it.code)
                            },
                            overlineContent = {
                                Text(it.host.name)
                            },
                            supportingContent = {
                                Text(it.players.toString())
                            }
                        ) {
                            Text(it.code)
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Light", widthDp = 1920, heightDp = 1080)
@Preview(widthDp = 1920, heightDp = 1080, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun RoomScreenPreview() {
    val state = GameState(
        roomsToJoin = mutableStateSetOf(
            ClientRoom("474853", 1, PlayerInfo(1, "Player 1"), 54, 0),
            ClientRoom("853474", 2, PlayerInfo(1, "Player 2"), 54, 0),
            ClientRoom("472004", 3, PlayerInfo(1, "Player 3"), 54, 0),
            ClientRoom("852003", 4, PlayerInfo(1, "Player 4"), 54, 0),
        )
    )
    CardGameTheme {
        Surface {
            RoomScreenLayout(state, {}, { _, _ -> }, {})
        }
    }
}