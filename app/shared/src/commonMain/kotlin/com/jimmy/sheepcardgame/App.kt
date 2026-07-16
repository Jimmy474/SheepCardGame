package com.jimmy.sheepcardgame

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.jimmy.sheepcardgame.ui.navigation.Routes
import com.jimmy.sheepcardgame.ui.navigation.RoutesConfig
import com.jimmy.sheepcardgame.ui.screens.AboutScreen
import com.jimmy.sheepcardgame.ui.screens.GameScreen
import com.jimmy.sheepcardgame.ui.screens.HomeScreen
import com.jimmy.sheepcardgame.ui.screens.HowToPlayScreen
import com.jimmy.sheepcardgame.ui.screens.RoomScreen
import com.jimmy.sheepcardgame.ui.screens.SettingsScreen
import com.jimmy.sheepcardgame.ui.theme.CardGameTheme
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFlexBoxApi::class)
@Composable
fun App() {
    val backStack = rememberNavBackStack(RoutesConfig, Routes.HomeRoute)

    fun navigate(route: Routes){
        backStack.add(route)
    }

    CardGameTheme {
        Surface {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                onBack = {
                    backStack.removeLastOrNull()
                },
                entryProvider = entryProvider{
                    entry<Routes.HomeRoute>{
                        HomeScreen(it, ::navigate)
                    }
                    entry<Routes.RoomRoute>{
                        RoomScreen(it, ::navigate)
                    }
                    entry<Routes.HowToPlayRoute>{
                        HowToPlayScreen(it, ::navigate)
                    }
                    entry<Routes.SettingsRoute>{
                        SettingsScreen(it, ::navigate)
                    }
                    entry<Routes.AboutRoute>{
                        AboutScreen(it, ::navigate)
                    }
                    entry<Routes.GameRoute>{
                        GameScreen(it, ::navigate)
                    }
                }
            )
        }
    }

    /*val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    CardGameTheme {
        Surface {
            Scaffold {
                Column(
                    modifier = Modifier
                        .padding(it)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .safeContentPadding()
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextField(tfState)
                    Button({ viewModel.connectToServer(tfState.text) }){ Text("Join Server") }
                    Button({ viewModel.addRandomCard() }){ Text("Add Random Card") }
                    Button({ showSheet = true }){ Text("Show Hand") }
                }

                if(showSheet){
                    ModalBottomSheet({
                        showSheet = false
                    }, sheetState = sheetState){
                        FlexBox(Modifier.fillMaxWidth().padding(8.dp), {
                            alignItems(FlexAlignItems.Center)
                            justifyContent(FlexJustifyContent.Center)
                            wrap(FlexWrap.Wrap)
                            gap(4.dp)
                        }){
                            state.player?.hand?.forEach { card ->
                                CardDisplay(card)
                            }
                        }
                    }
                }
            }
        }
    }*/
}
