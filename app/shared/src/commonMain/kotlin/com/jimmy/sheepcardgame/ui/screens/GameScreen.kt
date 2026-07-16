package com.jimmy.sheepcardgame.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.jimmy.sheepcardgame.*
import com.jimmy.sheepcardgame.data.*
import com.jimmy.sheepcardgame.ui.CardDisplay
import com.jimmy.sheepcardgame.ui.icons.*
import com.jimmy.sheepcardgame.ui.navigation.Routes
import com.jimmy.sheepcardgame.ui.spiralSunBurst
import com.jimmy.sheepcardgame.ui.theme.CardGameTheme
import com.jimmy.sheepcardgame.ui.theme.colorScheme
import io.github.alexzhirkevich.compottie.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.viewmodel.koinViewModel
import sheepcardgame.app.shared.generated.resources.Res

val LocalOnEvent = staticCompositionLocalOf<(GameEvents) -> Unit> {
    error("No LocalOnEvent provided")
}

@Composable
fun GameScreen(route: Routes.GameRoute, navigateTo: (Routes) -> Unit) {
    val viewModel = koinViewModel<GameViewModel>()
    val state by viewModel.state.collectAsState()
    val isStarted = state.clientRoom?.isStarted == true

    CompositionLocalProvider(LocalOnEvent provides viewModel::onEvent) {
        Scaffold {
            if (isStarted) GameBoard(state)
            else PreStartUI(state) { viewModel.startGame() }
        }
    }

}

@Composable
fun PreStartUI(state: GameState, onStart: () -> Unit) {

    if (state.player == null || state.clientRoom == null) return

    Row(Modifier.fillMaxSize().padding(4.dp), Arrangement.spacedBy(4.dp)) {
        Column(
            Modifier.fillMaxWidth(0.3f).fillMaxHeight().verticalScroll(rememberScrollState()).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                .padding(4.dp),
            Arrangement.spacedBy(8.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSecondaryContainer) {
                Text("CODE : ${state.clientRoom.code}")
                Text("HOST : ${state.clientRoom.host.name}")
                HorizontalDivider()
                Text("OPPONENTS")
                state.opponents.forEach { opponent ->
                    Text(opponent.info.name)
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                .padding(4.dp), Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally
        ) {
            val isEnabled = state.player.info.id == state.clientRoom.host.id
            ElevatedButton(
                onStart,
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
            ) {
                Text("START GAME")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFlexBoxApi::class, ExperimentalFoundationStyleApi::class)
private fun GameBoard(state: GameState) {
    if (state.player == null || state.clientRoom == null) return

    val onEvent = LocalOnEvent.current

    Column(Modifier.fillMaxSize().padding(4.dp), Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().weight(1f), Arrangement.spacedBy(4.dp)) {
            state.opponents.forEach { opponent ->
                Card(Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(4.dp),
                    ) {
                        stickyHeader {
                            OpponentInfo(opponent, ActionAgainstOpponent.getFromHand(state.player.hand), state.currentTurnPlayer == state.player.info.id)
                        }

                        items(opponent.info.flock.sheep) { sheep ->
                            DrawSheep(
                                SheepState.getFromSheepAndHand(sheep, state.player.hand, opponent.info.flock.isWolfProtected, opponent.info.flock.isWheatProtected),
                                opponent.info.id,
                                state.currentTurnPlayer == state.player.info.id
                            )
                        }
                    }
                }
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(8.dp), Arrangement.spacedBy(4.dp)) {
                LazyRow(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(4.dp),
                ) {
                    items(state.player.info.flock.sheep) { sheep ->
                        DrawSheep(
                            SheepState.getFromSheepAndHand(sheep, state.player.hand, wolfProtected = true, wheatProtected = true).copy(wheatCandidate = null, wolfCandidate = null),
                            state.player.info.id,
                            state.currentTurnPlayer == state.player.info.id
                        )
                    }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), Arrangement.spacedBy(4.dp), Alignment.CenterVertically){
                    state.player.hand.forEach { card ->
                        val isSelected = state.selectedCards.contains(card.id)
                        key(card.id) {
                            CardDisplay(
                                card,
                                isClickable = true,
                                isSelected = isSelected,
                            ) {
                                onEvent(GameEvents.ToggleCard(it.id))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), Alignment.CenterVertically) {
                    Icon(PlayingCardsIcon, contentDescription = "Deck cards count")
                    Text("${state.clientRoom.deck}")

                    Icon(PlayingCardsIcon, contentDescription = "Discard Pile cards count")
                    Text("${state.clientRoom.discardPile}")
                    if (state.player.info.flock.isWolfProtected) Icon(PetsIcon, "Wolf Protected")
                    if (state.player.info.flock.isWheatProtected) Icon(WheatIcon, "Wheat Protected")
                    AnimatedVisibility(visible = state.player.info.id == state.currentTurnPlayer) {
                        ElevatedButton({
                            onEvent(GameEvents.PlayCards)
                        }) {
                            Text("Play Cards")
                        }
                    }
                    AnimatedVisibility(visible = state.player.info.id == state.currentTurnPlayer) {
                        ElevatedButton(
                            { onEvent(GameEvents.EndTurn) },
                            colors = ButtonDefaults.elevatedButtonColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer),
                        ) {
                            Text("End Turn")
                        }
                    }
                }
            }
        }
    }

    when (state.dialog) {
        is GameDialogs.ExceedsMaxHandSize  -> ExceedsMaxHandSizeDialog(state.dialog.extraCards)
        is GameDialogs.DiscardConfirmation -> DiscardConfirmationDialog(state.dialog.card)
        is GameDialogs.Info                -> InfoDialog(state.dialog.message)
        is GameDialogs.SelectCards         -> SelectCardsDialog(state.dialog)
        is GameDialogs.SelectCoinFace      -> SelectCoinFaceDialog(state.dialog, state.player.hand.first { it.id == state.dialog.cardId })
        is GameDialogs.CoinFlip            -> CoinFlipDialog(state)
        is GameDialogs.SelectSheep         -> SelectSheepDialog(state.dialog)
        is GameDialogs.SelectCardsForSheep -> SelectCardsForSheepDialog(state.dialog)

        GameDialogs.None                   -> {}
    }
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
fun SelectCardsForSheepDialog(dialog: GameDialogs.SelectCardsForSheep) {
    val onEvent = LocalOnEvent.current
    val selectedCards = remember { mutableStateListOf<Card>() }

    AlertDialog(
        onDismissRequest = {}, confirmButton = {
        ElevatedButton(onClick = {
            if (!GameLogic.isValidSheep(selectedCards)) return@ElevatedButton
            onEvent(GameEvents.SubmitSelectedCardsForSheep(selectedCards.map { it.id }))
            onEvent(GameEvents.ChangeDialog(GameDialogs.None))
        }) {
            Text("Confirm")
        }
    }, title = { Text("Select Cards To Make A Valid Sheep") }, text = {
        FlexBox(Modifier.fillMaxWidth().padding(8.dp).verticalScroll(rememberScrollState()), {
            gap(4.dp)
            alignItems(FlexAlignItems.Center)
            justifyContent(FlexJustifyContent.Center)
            wrap(FlexWrap.Wrap)
        }) {
            dialog.cards.forEach { card ->
                CardDisplay(card, isClickable = true, isSelected = card in selectedCards, hasMenu = false) {
                    if (!selectedCards.remove(it)) {
                        selectedCards.add(it)
                    }
                }
            }
        }
    }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
fun SelectSheepDialog(dialog: GameDialogs.SelectSheep) {
    val onEvent = LocalOnEvent.current
    val selectedSheep = remember { mutableStateListOf<Pair<Sheep, SheepSide?>>() }

    AlertDialog(
        onDismissRequest = {}, confirmButton = {
        ElevatedButton(onClick = {
            onEvent(GameEvents.SubmitSelectedSheep(selectedSheep))
            onEvent(GameEvents.ChangeDialog(GameDialogs.None))
        }) {
            Text("Confirm")
        }
    }, title = { Text("Select ${dialog.amount} Sheep${if (dialog.selectHalf) " Half" else ""}") }, text = {
        FlexBox(Modifier.fillMaxWidth().padding(8.dp).verticalScroll(rememberScrollState()), {
            gap(4.dp)
            alignItems(FlexAlignItems.Center)
            justifyContent(FlexJustifyContent.Center)
            wrap(FlexWrap.Wrap)
        }) {
            dialog.sheep.forEach { sheep ->
                val selected = selectedSheep.firstOrNull { it.first == sheep }
                DrawSelectableSheep(sheep, selected != null, selected?.second, dialog.selectHalf) { s, side ->
                    if (selected != null) {
                        selectedSheep.remove(selected)
                    } else {
                        selectedSheep.add(s to side)
                    }
                }
            }
        }
    }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

@OptIn(ExperimentalResourceApi::class, ExperimentalCompottieApi::class)
@Composable
fun CoinFlipDialog(state: GameState) {
    val onEvent = LocalOnEvent.current
    val coinFlip = state.coinFlip ?: return

    val playerName = if (coinFlip.attacker == state.player!!.info.id) "You" else state.opponents.firstOrNull { it.info.id == coinFlip.attacker }?.info?.name ?: "Unknown"
    val opponentName = if (coinFlip.target == state.player.info.id) "You" else state.opponents.firstOrNull { it.info.id == coinFlip.target }?.info?.name ?: "Unknown"

    val headsSpec = LottieCompositionSpec.Resource(Res.getUri("files/Heads.json"))
    val tailsSpec = LottieCompositionSpec.Resource(Res.getUri("files/Tails.json"))

    val headsComposition = rememberLottieComposition { headsSpec }
    val tailsComposition = rememberLottieComposition { tailsSpec }

    val activeComposition = when (coinFlip.currentResult) {
        true  -> headsComposition.value
        false -> tailsComposition.value
        else  -> null
    }

    val animatable = rememberLottieAnimatable()
    var isAnimationFinished by remember { mutableStateOf(false) }

    LaunchedEffect(coinFlip.iteration) {
        if (coinFlip.currentResult != null) {
            if (activeComposition != null) {
                isAnimationFinished = false
                animatable.animate(
                    composition = activeComposition, initialProgress = 0f
                ) {
                    isAnimationFinished = true
                }
            } else {
                isAnimationFinished = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = {}, confirmButton = {
        if (coinFlip.target == state.player.info.id && coinFlip.currentResult == null) {
            ElevatedButton(onClick = {
                onEvent(GameEvents.FlipCoin)
            }) {
                Text("Flip Coin")
            }
        }
        if (coinFlip.currentResult != null) {
            if (!coinFlip.canReFlip) {
                ElevatedButton(onClick = {
                    onEvent(GameEvents.ChangeDialog(GameDialogs.None))
                    val isWinner = if (coinFlip.playerChoice == coinFlip.currentResult) coinFlip.attacker == state.player.info.id else coinFlip.target == state.player.info.id
                    if (isWinner) {
                        onEvent(GameEvents.EndCoinFlip)
                    }
                }) { Text("Close") }
            } else if (state.player.info.id !in coinFlip.skippedReFlip) {
                state.player.hand.firstOrNull { it is Card.SpecialCard && it.specialType == SpecialType.ReFlip }?.let {
                    Row {
                        ElevatedButton(onClick = { onEvent(GameEvents.ReFlip(it.id)) }) { Text("Consume ReFlip Card & Re-Flip") }
                        OutlinedButton(onClick = { onEvent(GameEvents.SkipReFlip) }) { Text("Skip Re-Flip") }
                    }
                }
            }
        }
    }, title = { Text("Coin Flip") }, text = {
        if (headsComposition.isLoading || tailsComposition.isLoading) {
            CircularProgressIndicator()
        } else {
            Column {
                Text("$playerName Played")
                CardDisplay(coinFlip.goldCard)
                Text("Against $opponentName And Called ${if (coinFlip.playerChoice) "Heads" else "Tails"}, Result Is... ")

                coinFlip.currentResult?.let { result ->
                    if (activeComposition == null) {
                        CircularProgressIndicator()
                    } else {
                        Image(
                            painter = rememberLottiePainter(
                            composition = activeComposition, progress = { animatable.progress }), contentDescription = "Coin Flip Animation")
                    }

                    if (isAnimationFinished) {
                        Text("${if (result == coinFlip.playerChoice) playerName else opponentName} Wins.")
                    }
                }
            }
        }
    }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

@Composable
fun SelectCoinFaceDialog(dialog: GameDialogs.SelectCoinFace, card: Card) {
    val onEvent = LocalOnEvent.current
    var isHeads: Boolean? by remember { mutableStateOf(null) }

    AlertDialog(onDismissRequest = {
        onEvent(GameEvents.ChangeDialog(GameDialogs.None))
    }, confirmButton = {
        ElevatedButton(enabled = isHeads != null, onClick = {
            onEvent(GameEvents.RequestCoinFlip(card, dialog.opponent.id, isHeads!!))
            onEvent(GameEvents.ChangeDialog(GameDialogs.None))
        }) {
            Text("Confirm")
        }
    }, dismissButton = {
        OutlinedButton(onClick = { onEvent(GameEvents.ChangeDialog(GameDialogs.None)) }) {
            Text("Cancel")
        }
    }, title = { Text("Select Coin Face") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("To Play card against ${dialog.opponent.name}")
            Row(Modifier, horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isHeads = true }, Modifier.size(80.dp)) {
                    Icon(
                        CoinHeadIcon,
                        "Heads",
                        Modifier.fillMaxSize(),
                        tint = if (isHeads == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    )
                }
                CardDisplay(card)
                IconButton(onClick = { isHeads = false }, Modifier.size(80.dp)) {
                    Icon(
                        CoinTailIcon,
                        "Tails",
                        Modifier.fillMaxSize(),
                        tint = if (isHeads == false) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }
        }
    })
}

@OptIn(ExperimentalFlexBoxApi::class, ExperimentalFoundationStyleApi::class)
@Composable
fun SelectCardsDialog(dialog: GameDialogs.SelectCards) {
    val onEvent = LocalOnEvent.current
    val selectedCards = remember { mutableStateListOf<Int>() }

    val style = Style {
        border(4.dp, Color.White)
        shape(RoundedCornerShape(4.dp))
        clip(true)

        hovered {
            animate {
                border(4.dp, colorScheme.tertiary)
            }
        }

        selected {
            animate {
                border(4.dp, colorScheme.inversePrimary)
            }
        }
    }

    AlertDialog(onDismissRequest = {
        onEvent(GameEvents.ChangeDialog(GameDialogs.None))
    }, confirmButton = {
        ElevatedButton(enabled = selectedCards.size == dialog.amount, onClick = {
            if (selectedCards.size != dialog.amount) return@ElevatedButton
            onEvent(GameEvents.SubmitSelectedCards(selectedCards.toList(), dialog.cardId, dialog.opponentId))
            onEvent(GameEvents.ChangeDialog(GameDialogs.None))
        }) {
            Text("Confirm")
        }
    }, dismissButton = {
        OutlinedButton(onClick = { onEvent(GameEvents.ChangeDialog(GameDialogs.None)) }) {
            Text("Cancel")
        }
    }, title = { Text("Select ${dialog.amount} Cards") }, text = {
        FlexBox(Modifier.fillMaxWidth().padding(8.dp).verticalScroll(rememberScrollState()), {
            gap(4.dp)
            alignItems(FlexAlignItems.Center)
            justifyContent(FlexJustifyContent.Center)
            wrap(FlexWrap.Wrap)
        }) {
            dialog.cards.forEach { cardId ->
                val interactionSource = remember { MutableInteractionSource() }
                val styleState = rememberUpdatedStyleState(interactionSource) {
                    it.isSelected = cardId in selectedCards
                }
                Box(
                    Modifier.width(80.dp).aspectRatio(5f / 7f).hoverable(interactionSource).styleable(styleState, style).clickable {
                            if (!selectedCards.remove(cardId)) selectedCards.add(cardId)
                        }) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        spiralSunBurst(Color(0xFFf299ba), Color(0xFFf8c7d5), 20, 0f, 90f)
                    }
                }
            }
        }
    })
}

@Composable
fun InfoDialog(message: String) {
    val onEvent = LocalOnEvent.current
    AlertDialog(
        onDismissRequest = {
            onEvent(GameEvents.ChangeDialog(GameDialogs.None))
        },
        confirmButton = {
            ElevatedButton(onClick = { onEvent(GameEvents.ChangeDialog(GameDialogs.None)) }) {
                Text("OK")
            }
        },
        text = { Text(message) },
    )
}

@Composable
fun ExceedsMaxHandSizeDialog(extraCards: Int) {
    val onEvent = LocalOnEvent.current
    AlertDialog(
        onDismissRequest = {
            onEvent(GameEvents.ChangeDialog(GameDialogs.None))
        },
        confirmButton = {
            ElevatedButton(onClick = { onEvent(GameEvents.ChangeDialog(GameDialogs.None)) }) {
                Text("OK")
            }
        },
        title = { Text("Exceeds Max Hand Size") },
        text = { Text("You have $extraCards extra cards in your hand, either discard them or use them to continue. If you fail to do so before the timer ends random cards from your hand will be discarded instead to end your turn.") },
    )
}

@Composable
fun DiscardConfirmationDialog(card: Card) {
    val onEvent = LocalOnEvent.current
    AlertDialog(onDismissRequest = {
        onEvent(GameEvents.ChangeDialog(GameDialogs.None))
    }, confirmButton = {
        ElevatedButton(onClick = {
            onEvent(GameEvents.ChangeDialog(GameDialogs.None))
            onEvent(GameEvents.Discard(card))
        }) {
            Text("OK")
        }
    }, dismissButton = {
        OutlinedButton(onClick = { onEvent(GameEvents.ChangeDialog(GameDialogs.None)) }) {
            Text("Cancel")
        }
    }, title = { Text("Confirm Discard") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Are you sure you want to discard this card?")
            CardDisplay(card)
        }
    })
}

data class ActionAgainstOpponent(
    val yoink: Int?,
    val goldYoink: Int?,
    val goldLure2Sheep: Int?,
    val goldHalve2Sheep: Int?,
    val goldRemove2Sheep: Int?,
    val goldRecoverSheep: Int?,
) {
    companion object {
        fun getFromHand(hand: List<Card>) = ActionAgainstOpponent(
            yoink = hand.filterIsInstance<Card.SpecialCard>().firstOrNull { it.specialType == SpecialType.Yoink }?.id,
            goldYoink = hand.filterIsInstance<Card.GoldCard>().firstOrNull { it.goldCardType == GoldCardType.Yoink }?.id,
            goldLure2Sheep = hand.filterIsInstance<Card.GoldCard>().firstOrNull { it.goldCardType == GoldCardType.Lure }?.id,
            goldHalve2Sheep = hand.filterIsInstance<Card.GoldCard>().firstOrNull { it.goldCardType == GoldCardType.Halve }?.id,
            goldRemove2Sheep = hand.filterIsInstance<Card.GoldCard>().firstOrNull { it.goldCardType == GoldCardType.Remove }?.id,
            goldRecoverSheep = hand.filterIsInstance<Card.GoldCard>().firstOrNull { it.goldCardType == GoldCardType.Recover }?.id
        )
    }
}

@Composable
fun OpponentInfo(opponent: Opponent, aao: ActionAgainstOpponent, hasMenu: Boolean) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val onEvent = LocalOnEvent.current

    Box {
        Column(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp)).padding(8.dp)
                .clickable { isMenuExpanded = !isMenuExpanded }, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(opponent.info.name)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(PlayingCardsIcon, contentDescription = "${opponent.info.name} cards count")
                Text(opponent.numCards.toString())
                if (opponent.info.flock.isWolfProtected) Icon(PetsIcon, "Wolf Protected")
                if (opponent.info.flock.isWheatProtected) Icon(WheatIcon, "Wheat Protected")
            }
        }

        DropdownMenu(
            expanded = isMenuExpanded && hasMenu, onDismissRequest = { isMenuExpanded = false }) {
            aao.yoink?.let {
                DropdownMenuItem(text = { Text("Yoink 2 Cards") }, onClick = {
                    onEvent(GameEvents.Yoink(opponent.info.id, it))
                })
            }
            aao.goldYoink?.let {
                DropdownMenuItem(text = { Text("Yoink Entire Hand") }, trailingIcon = { Icon(CoinHeadIcon, "Requires Flipping A Coin") }, onClick = {
                    onEvent(GameEvents.ChangeDialog(GameDialogs.SelectCoinFace(it, opponent.info)))
                })
            }
            aao.goldLure2Sheep?.let {
                DropdownMenuItem(text = { Text("Lure 2 Sheep") }, trailingIcon = { Icon(CoinHeadIcon, "Requires Flipping A Coin") }, onClick = {
                    onEvent(GameEvents.ChangeDialog(GameDialogs.SelectCoinFace(it, opponent.info)))
                })
            }
            aao.goldHalve2Sheep?.let {
                DropdownMenuItem(text = { Text("Halve 2 Sheep") }, trailingIcon = { Icon(CoinHeadIcon, "Requires Flipping A Coin") }, onClick = {
                    onEvent(GameEvents.ChangeDialog(GameDialogs.SelectCoinFace(it, opponent.info)))
                })
            }
            aao.goldRemove2Sheep?.let {
                DropdownMenuItem(text = { Text("Remove 2 Sheep") }, trailingIcon = { Icon(CoinHeadIcon, "Requires Flipping A Coin") }, onClick = {
                    onEvent(GameEvents.ChangeDialog(GameDialogs.SelectCoinFace(it, opponent.info)))
                })
            }
            aao.goldRecoverSheep?.let {
                DropdownMenuItem(text = { Text("Recover 1 Sheep") }, trailingIcon = { Icon(CoinHeadIcon, "Requires Flipping A Coin") }, onClick = {
                    onEvent(GameEvents.ChangeDialog(GameDialogs.SelectCoinFace(it, opponent.info)))
                })
            }
        }
    }
}

data class SheepState(
    val sheep: Sheep,
    val deFrankenCandidates: List<Card.SheepCard> = emptyList(),
    val dePaintCandidates: List<Card.SheepCard> = emptyList(),
    val deRainbowCandidates: List<Card.SheepCard> = emptyList(),
    val wolfCandidate: Int?,
    val wheatCandidate: Int?,
) {
    companion object {
        fun getFromSheepAndHand(sheep: Sheep, hand: List<Card>, wolfProtected: Boolean, wheatProtected: Boolean) = SheepState(
            sheep,
            deFrankenCandidates = sheep.deFrankenCandidates(hand),
            dePaintCandidates = sheep.dePaintCandidates(hand),
            deRainbowCandidates = sheep.deRainbowCandidate(hand),
            wolfCandidate = if (wolfProtected) null else hand.filterIsInstance<Card.SpecialCard>().firstOrNull { it.specialType == SpecialType.Wolf }?.id,
            wheatCandidate = if (wheatProtected) null else hand.filterIsInstance<Card.SpecialCard>().firstOrNull { it.specialType == SpecialType.Wheat }?.id,
        )
    }
}

@Composable
private fun DrawSheep(state: SheepState, owner: Long, hasMenu: Boolean) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val onEvent = LocalOnEvent.current

    Box(Modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { isMenuExpanded = true })
        }.pointerInput(Unit) {
            awaitEachGesture {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        event.changes.forEach { it.consume() }
                        isMenuExpanded = true
                    }
                }
            }
        }) {
        state.sheep.modifier?.let { card ->
            CardDisplay(card, Modifier.align(Alignment.TopCenter))
        }
        Row(
            Modifier.align(Alignment.Center).then(
                if (state.sheep.modifier != null) Modifier.padding(top = 25.dp)
                else Modifier
            ),
            Arrangement.spacedBy(2.dp),
        ) {
            CardDisplay(state.sheep.head, Modifier.rotate(if (state.sheep.isFrankenButts) 180f else 0f))
            CardDisplay(state.sheep.butt, Modifier.rotate(if (state.sheep.isFrankenHeads) 180f else 0f))
        }
        DropdownMenu(
            expanded = isMenuExpanded && hasMenu, onDismissRequest = { isMenuExpanded = false }) {
            DropdownMenuItem(text = { Text("View") }, onClick = {
                isMenuExpanded = false
                // TODO: show zoomed in view
            })
            if (state.deFrankenCandidates.isNotEmpty()) HorizontalDivider()
            state.deFrankenCandidates.forEach { candidate ->
                DropdownMenuItem(text = { Text("Fix Franken Using ${candidate.name}") }, onClick = {
                    isMenuExpanded = false
                    onEvent(GameEvents.FixSheep(FixSheepType.Franken, state.sheep, candidate.id, owner))
                })
            }
            if (state.dePaintCandidates.isNotEmpty()) HorizontalDivider()
            state.dePaintCandidates.forEach { candidate ->
                DropdownMenuItem(text = { Text("Fix Paint Using ${candidate.name}") }, onClick = {
                    isMenuExpanded = false
                    onEvent(GameEvents.FixSheep(FixSheepType.Paint, state.sheep, candidate.id, owner))
                })
            }
            if (state.deRainbowCandidates.isNotEmpty()) HorizontalDivider()
            state.deRainbowCandidates.forEach { candidate ->
                DropdownMenuItem(text = { Text("Fix Rainbow Using ${candidate.name}") }, onClick = {
                    isMenuExpanded = false
                    onEvent(GameEvents.FixSheep(FixSheepType.Rainbow, state.sheep, candidate.id, owner))
                })
            }
            state.wolfCandidate?.let {
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Wolf") }, onClick = {
                    isMenuExpanded = false
                    onEvent(GameEvents.Wolf(state.sheep, owner, it))
                })
            }
            state.wheatCandidate?.let {
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Wheat") }, onClick = {
                    isMenuExpanded = false
                    onEvent(GameEvents.Wheat(state.sheep, owner, it))
                })
            }
        }
    }
}

@Composable
private fun DrawSelectableSheep(sheep: Sheep, selected: Boolean, selectedSide: SheepSide?, selectHalf: Boolean, onSelect: (Sheep, SheepSide?) -> Unit) {
    val borderColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)

    OutlinedCard(border = BorderStroke(2.dp, borderColor)) {
        Box(Modifier.clickable(!selectHalf) { onSelect(sheep, null) }) {
            sheep.modifier?.let { card ->
                CardDisplay(card, Modifier.align(Alignment.TopCenter))
            }
            Row(
                Modifier.align(Alignment.Center).then(
                    if (sheep.modifier != null) Modifier.padding(top = 25.dp)
                    else Modifier
                ),
                Arrangement.spacedBy(2.dp),
            ) {
                CardDisplay(
                    sheep.head,
                    Modifier.rotate(if (sheep.isFrankenButts) 180f else 0f),
                    isClickable = selectHalf,
                    isSelected = selected && selectedSide == SheepSide.Front,
                    onSelected = { onSelect(sheep, SheepSide.Front) },
                )
                CardDisplay(
                    sheep.butt,
                    Modifier.rotate(if (sheep.isFrankenHeads) 180f else 0f),
                    isClickable = selectHalf,
                    isSelected = selected && selectedSide == SheepSide.Back,
                    onSelected = { onSelect(sheep, SheepSide.Back) },
                )
            }
        }
    }
}

@Composable
@Preview(widthDp = 800, heightDp = 640)
@Preview(widthDp = 800, heightDp = 640, uiMode = UI_MODE_NIGHT_YES)
fun GameStatePreview() {
    val redSheep = Sheep(Card.SheepCard(1, SheepSide.Front, SheepColor.Red), Card.SheepCard(2, SheepSide.Back, SheepColor.Red))
    val blueSheep = Sheep(Card.SheepCard(3, SheepSide.Front, SheepColor.Blue), Card.SheepCard(4, SheepSide.Back, SheepColor.Blue))
    val limeRainbowSheep = Sheep(Card.SheepCard(5, SheepSide.Front, SheepColor.Lime), Card.SheepCard(6, SheepSide.Back, SheepColor.Rainbow))
    val rainbowOrangeSheep = Sheep(Card.SheepCard(7, SheepSide.Front, SheepColor.Rainbow), Card.SheepCard(8, SheepSide.Back, SheepColor.Orange))
    val pinkYellowFrankenSheep =
        Sheep(Card.SheepCard(9, SheepSide.Front, SheepColor.Pink), Card.SheepCard(10, SheepSide.Front, SheepColor.Yellow), Card.ModifierCard(11, ModifierType.Franken))
    val blackWhiteFrankenSheep =
        Sheep(Card.SheepCard(12, SheepSide.Back, SheepColor.Black), Card.SheepCard(13, SheepSide.Back, SheepColor.White), Card.ModifierCard(14, ModifierType.Franken))
    val brownBeigePaintSheep =
        Sheep(Card.SheepCard(15, SheepSide.Front, SheepColor.Brown), Card.SheepCard(16, SheepSide.Back, SheepColor.Beige), Card.ModifierCard(17, ModifierType.Paint))

    val state = GameState(
        player = Player(
            PlayerInfo(1, "Player", Flock(listOf(redSheep, blueSheep, limeRainbowSheep, pinkYellowFrankenSheep))), listOf(
                Card.SheepCard(1, SheepSide.Front, SheepColor.Red),
                Card.SheepCard(2, SheepSide.Back, SheepColor.Red),
                Card.SpecialCard(18, SpecialType.Yoink),
                Card.GoldCard(19, GoldCardType.Yoink),
                Card.GoldCard(19, GoldCardType.Remove),
                Card.GoldCard(19, GoldCardType.Halve),
                Card.GoldCard(19, GoldCardType.Recover),
                Card.GoldCard(19, GoldCardType.Lure),
            )
        ),
        selectedCards = listOf(2),
        opponents = setOf(
            Opponent(PlayerInfo(2, "Opponent 1", Flock(listOf(pinkYellowFrankenSheep, blackWhiteFrankenSheep))), 8),
            Opponent(PlayerInfo(3, "Opponent 2", Flock(listOf(rainbowOrangeSheep, blackWhiteFrankenSheep, blackWhiteFrankenSheep))), 5),
            Opponent(PlayerInfo(4, "Opponent 3", Flock(listOf(brownBeigePaintSheep))), 3),
        ),
        clientRoom = ClientRoom(
            code = "123456",
            players = 4,
            host = PlayerInfo(1, "Player", Flock(listOf())),
            deck = 54,
            discardPile = 0,
            isStarted = true,
        ),
        currentTurnPlayer = 1
    )

    CardGameTheme {
        Surface {
            CompositionLocalProvider(LocalOnEvent provides {}) {
                Scaffold {
                    if (state.clientRoom?.isStarted == true) GameBoard(state)
                    else PreStartUI(state) {}
                }
            }
        }
    }
}