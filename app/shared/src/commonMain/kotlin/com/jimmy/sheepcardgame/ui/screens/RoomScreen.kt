package com.jimmy.sheepcardgame.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jimmy.sheepcardgame.GameViewModel
import com.jimmy.sheepcardgame.ui.icons.PlayingCardsIcon
import com.jimmy.sheepcardgame.ui.icons.RefreshIcon
import com.jimmy.sheepcardgame.ui.navigation.Routes
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoomScreen(route: Routes.RoomRoute, navigateTo: (Routes) -> Unit) {

    val viewModel = koinViewModel<GameViewModel>()
    val state by viewModel.state.collectAsState()

    val name = rememberTextFieldState()
    val code = rememberTextFieldState()
    var isError by remember { mutableStateOf(false) }

    Scaffold {
        Column(Modifier.padding(it).fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            OutlinedTextField(
                name,
                label = { Text("Name") },
                placeholder = { Text("Enter your name") },
                lineLimits = TextFieldLineLimits.SingleLine,
                isError = isError,
                supportingText = {
                    if(isError){
                        Text("Name must be not empty or blank.")
                    }
                },
                inputTransformation = {
                    isError = false
                }
            )
            ElevatedButton({
                if(name.text.isBlank()) {
                    isError = true
                    return@ElevatedButton
                }
                viewModel.connectToServerCreateRoom(name.text.toString(), { navigateTo(Routes.GameRoute) },{})
            }){ Text("Create Room") }

            HorizontalDivider()

            Text("Or join a room")
            OutlinedTextField(
                code,
                label = { Text("Room Code") },
                placeholder = { Text("Enter room code") },
                lineLimits = TextFieldLineLimits.SingleLine,
                inputTransformation = {
                    isError = false
                }
            )
            ElevatedButton({
                viewModel.connectToServerJoinRoom(name.text.toString(), code.text.toString(), { navigateTo(Routes.GameRoute) }, {})
            }){ Text("Join Room") }

            HorizontalDivider()

            LazyColumn{
                stickyHeader {
                    Row {
                        IconButton({
                            viewModel.fetchRoomsList()
                        }){
                            Icon(RefreshIcon, contentDescription = "Settings")
                        }
                    }
                }
                items(state.roomsToJoin){
                    ListItem({
                        viewModel.connectToServerJoinRoom(name.text.toString(), it.code, { navigateTo(Routes.GameRoute) }, {})
                    }, overlineContent = {
                        Text(it.host.name)
                    }, supportingContent = {
                        Text(it.players.toString())
                    }){
                        Text(it.code)
                    }
                }
            }
        }
    }
}