package com.jimmy.sheepcardgame.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jimmy.sheepcardgame.*
import com.jimmy.sheepcardgame.data.*
import com.jimmy.sheepcardgame.ui.*
import com.jimmy.sheepcardgame.ui.icons.*
import com.jimmy.sheepcardgame.ui.theme.CardGameTheme
import io.github.alexzhirkevich.compottie.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import sheepcardgame.app.shared.generated.resources.Res
import sheepcardgame.app.shared.generated.resources.wheat
import sheepcardgame.app.shared.generated.resources.wolf

val LocalOnEvent = staticCompositionLocalOf<(GameScreenEvents) -> Unit> {
    error("No LocalOnEvent provided")
}

class DragDropState {
    var isDragging by mutableStateOf(false)
    var currentPosition by mutableStateOf(Offset.Zero)
    var draggedCards by mutableStateOf<Set<Card>>(emptySet())

    val dropZones = mutableMapOf<Any, DropZoneInfo>()

    fun onDrop() {
        val validZone = dropZones.values
            .filter { zone -> zone.bounds.contains(currentPosition) && zone.predicate(draggedCards) }
            .minByOrNull { zone -> zone.bounds.width * zone.bounds.height }

        validZone?.onDrop?.invoke(draggedCards)

        reset()
    }

    fun reset() {
        isDragging = false
        draggedCards = emptySet()
        currentPosition = Offset.Zero
    }
}

data class DropZoneInfo(
    var bounds: Rect,
    val predicate: (Set<Card>) -> Boolean,
    val onDrop: (Set<Card>) -> Unit
)

val LocalDragDropState = compositionLocalOf { DragDropState() }

@Composable
fun GameScreen(exit: () -> Unit) {
    val viewModel = koinViewModel<GameViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dialogs by viewModel.dialogs.collectAsStateWithLifecycle()
    val isStarted = state.clientRoom?.isStarted == true
    if (state.player == null) return

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectFromServer()
        }
    }

    CompositionLocalProvider(LocalOnEvent provides viewModel::onEventFixed) {
        GameDragDropOverlay {
            if (isStarted) GameBoardWrapper(state)
            else PreStartUI(state, { viewModel.startGame() }, exit)
            DisplayDialogs(state, dialogs, exit)
        }
    }

}

@Composable
fun DisplayDialogs(state: GameState, dialogs: List<GameDialogs>, exit: () -> Unit) {
    dialogs.firstOrNull()?.let { dialog ->
        when (dialog) {
            is GameDialogs.DiscardConfirmation -> DiscardConfirmationDialog(dialog.cards)
            GameDialogs.ExitConfirmation       -> ExitConfirmationDialog(exit)
            is GameDialogs.Info                -> InfoDialog(dialog.message)
            is GameDialogs.FinalRound                -> InfoDialog("The Final Round Begins now. Each player gets one last turn.")
            is GameDialogs.SelectCards         -> SelectCardsDialog(dialog)
            GameDialogs.CoinFlip               -> CoinFlipDialog(state)
            is GameDialogs.SelectSheep         -> SelectSheepDialog(dialog)
            is GameDialogs.SelectCardsForSheep -> SelectCardsForSheepDialog(dialog)
            is GameDialogs.GameOver            -> GameOverDialog(state, dialog)
            is GameDialogs.SelectOpponent      -> SelectOpponentDialog(state, dialog)
            is GameDialogs.SelectOpponentSheep -> SelectOpponentSheepDialog(state, dialog)
            is GameDialogs.ExpandedView        -> ExpandedViewDialog(dialog)
        }
    }
}

@Composable
fun GameDragDropOverlay(content: @Composable () -> Unit) {
    val state = remember { DragDropState() }

    CompositionLocalProvider(LocalDragDropState provides state) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            if (state.isDragging && state.draggedCards.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                state.currentPosition.x.toInt(),
                                state.currentPosition.y.toInt()
                            )
                        }
                ) {
                    state.draggedCards.forEachIndexed { i, card ->
                        CardDisplay(card = card, Modifier.padding(start = (i * 24).dp).dropShadow(RoundedCornerShape(4.dp), Shadow(2.dp, Color.Black, 0.dp, DpOffset(1.dp, 1.dp))))
                    }
                }
            }
        }
    }
}

@Composable
fun GameBoardWrapper(state: GameState) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val lazyState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.events.size) {
        lazyState.animateScrollToItem(0)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            gesturesEnabled = false,
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerState = drawerState) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        FilledIconButton({scope.launch{drawerState.close()}}, Modifier.padding(8.dp)) {
                            Icon(CloseIcon, "Close Events List")
                        }
                        LazyColumn(Modifier.fillMaxSize().padding(8.dp), lazyState, contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(state.events, { _, it -> it.id }) { i, it ->
                                DrawGameEventItem(it)
                                if (i != state.events.size - 1) HorizontalDivider(Modifier.padding(top = 4.dp).animateItem())
                            }
                        }
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                GameBoard(state, drawerState)
            }
        }
    }
}

@Composable
private fun LazyItemScope.DrawGameEventItem(event: GameEvents) {
    val playerStyle = SpanStyle(color = MaterialTheme.colorScheme.primaryContainer)
    val opponentStyle = SpanStyle(color = MaterialTheme.colorScheme.secondaryContainer)
    val importantStyle = SpanStyle(color = MaterialTheme.colorScheme.tertiaryContainer)
    val inlineContent = mutableMapOf<String, InlineTextContent>()

    val text = buildAnnotatedString {
        when (event) {
            is GameEvents.CoinFlipResult -> {
                withStyle(playerStyle) { append(event.attacker) }
                append(" Choose ")
                withStyle(importantStyle) {
                    appendInlineContent("playerChoice".also {
                        inlineContent[it] = InlineTextContent(Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.TextCenter)) {
                            Icon(if (event.playerChoice) CoinHeadIcon else CoinTailIcon, event.playerChoice.headTail(), tint = MaterialTheme.colorScheme.tertiaryContainer)
                        }
                    }, event.playerChoice.headTail())
                }
                append(", The Result is ")
                withStyle(importantStyle) {
                    appendInlineContent("result".also {
                        inlineContent[it] = InlineTextContent(Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.TextCenter)) {
                            Icon(if (event.result) CoinHeadIcon else CoinTailIcon, event.result.headTail(), tint = MaterialTheme.colorScheme.tertiaryContainer)
                        }
                    }, event.result.headTail())
                }
                append(", ")
                if (event.playerChoice == event.result) withStyle(playerStyle) { append(event.attacker) }
                else withStyle(opponentStyle) { append(event.target) }
                append(" Won")
            }

            is GameEvents.ReFlipped      -> {
                withStyle(playerStyle) { append(event.player) }
                append(" Invoked A Re Flip")
            }

            is GameEvents.DiscardedCards -> {
                withStyle(playerStyle) { append(event.player) }
                append(" Discarded ")
                withStyle(importantStyle) { append("${event.cardsAmount}") }
                append(" Card(s)")
            }

            is GameEvents.DrawCards      -> {
                withStyle(playerStyle) { append(event.player) }
                append(" Drew ")
                withStyle(importantStyle) { append("${event.cardsAmount}") }
                append(" Card(s)")
            }

            is GameEvents.FixSheep       -> {
                withStyle(playerStyle) { append(event.player) }
                append(" ${event.fixSheepType.title} ")
                withStyle(importantStyle) { append(event.sheep) }
                append(" Sheep In The Flock Of ")
                withStyle(opponentStyle) { append(event.opponent ?: "Their Own") }
            }

            is GameEvents.PlaceSheep     -> {
                withStyle(playerStyle) { append(event.player) }
                append(" Placed ")
                withStyle(importantStyle) { append(event.sheep) }
                append(" Sheep")
            }

            is GameEvents.PlayGoldCard   -> {
                withStyle(playerStyle) { append(event.player) }
                append(" Played ")
                withStyle(importantStyle) { append(event.goldCardType.name) }
                append(" Gold Card Against ")
                withStyle(opponentStyle) { append(event.opponent) }
            }

            is GameEvents.TurnChange     -> {
                append("It Is ")
                withStyle(playerStyle) { append(event.player) }
                append("'s Turn Now")
            }

            is GameEvents.WheatWolf      -> {
                withStyle(playerStyle) { append(event.player) }
                append(if (event.isWheat) " Wheated" else " Wolfed")
                withStyle(importantStyle) { append(" ${event.sheep}") }
                append(" Sheep From ")
                withStyle(opponentStyle) { append(event.opponent) }
                append("'s Flock")
            }

            is GameEvents.YoinkCards     -> {
                withStyle(playerStyle) { append(event.player) }
                append(" Yoiked ")
                withStyle(importantStyle) { append("${event.cardsAmount}") }
                append(" Card(s) From ")
                withStyle(opponentStyle) { append(event.opponent) }
            }

            is GameEvents.GoldCardResult -> {
                withStyle(playerStyle) { append(event.player) }
                append(
                    when (event.goldCardType) {
                        GoldCardType.Remove  -> " Removed "
                        GoldCardType.Yoink   -> " Yoiked "
                        GoldCardType.Lure    -> " Lured "
                        GoldCardType.Halve   -> " Halved "
                        GoldCardType.Recover -> " Recovered "
                    }
                )
                withStyle(importantStyle) { append("${event.amount}") }
                append(if (event.goldCardType == GoldCardType.Yoink) " Card(s) From " else " Sheep From ")
                withStyle(opponentStyle) { append(event.opponent) }
                append(if (event.goldCardType == GoldCardType.Yoink) "'s Hand" else "'s Flock ")
            }
        }
    }
    SmallListItem(
        Modifier.fillMaxWidth().animateItem(),
        headline = {
            Text(text, inlineContent = inlineContent)
        }
    )
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
fun PreStartUI(state: GameState, onStart: () -> Unit, onExit: () -> Unit) {

    val onEvent = LocalOnEvent.current

    var editableSettings by remember(state.settings) { mutableStateOf(state.settings) }

    val hasChanges by remember(state.settings) {
        derivedStateOf { editableSettings != state.settings }
    }

    if (state.player == null || state.clientRoom == null) return
    val isHost = state.player.info.id == state.clientRoom.host.id

    Row(Modifier.fillMaxSize().padding(4.dp), Arrangement.spacedBy(4.dp)) {
        Card(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(8.dp), Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally) {
                Text("Opponents", style = MaterialTheme.typography.displaySmallEmphasized)
                FlexBox(Modifier.fillMaxWidth(), {
                    gap(8.dp)
                    wrap(FlexWrap.Wrap)
                }) {
                    state.opponents.forEach { opponent ->
                        Text(
                            opponent.info.name,
                            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp)).padding(4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Text("Previous Games", style = MaterialTheme.typography.displaySmallEmphasized)
                FlexBox(Modifier.fillMaxWidth(), {
                    gap(8.dp)
                    wrap(FlexWrap.Wrap)
                }) {
                    state.previousGameScores.forEach {
                        Scorecard(it)
                    }
                }
            }
        }
        ElevatedCard {
            Column(
                Modifier.fillMaxHeight().padding(8.dp),
                Arrangement.spacedBy(8.dp),
                Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedCard(
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    ) {
                        SmallListItem(
                            Modifier.padding(8.dp),
                            leadingIcon = {
                                Icon(TagIcon, "Code", Modifier.size(32.dp))
                            },
                            headline = { Text("CODE") },
                            text = { Text(state.clientRoom.code) }
                        )
                    }
                    ElevatedCard(
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    ) {
                        SmallListItem(
                            Modifier.padding(8.dp),
                            leadingIcon = {
                                Icon(CrownIcon, "Host", Modifier.size(32.dp))
                            },
                            headline = { Text("HOST") },
                            text = { Text(state.clientRoom.host.name, Modifier.basicMarquee()) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Settings", style = MaterialTheme.typography.displaySmallEmphasized)
                    AnimatedVisibility(hasChanges && isHost) {
                        FilledTonalButton({
                            onEvent(GameScreenEvents.SaveRoomSettings(editableSettings))
                        }) { Text("Save") }
                    }
                }

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally) {
                    SettingsField(editableSettings.initialHandSize, {editableSettings = editableSettings.copy(initialHandSize = it)}, isHost, "Initial Hand Size", "Number of cards in the initial hand")
                    SettingsField(editableSettings.drawOnEachTurn, {editableSettings = editableSettings.copy(drawOnEachTurn = it)}, isHost, "Draw On Each Turn", "Number of cards to draw at the start of each turn")
                    SettingsField(editableSettings.minHandSize, {editableSettings = editableSettings.copy(minHandSize = it)}, isHost, "Min Hand Size", "Minimum number of cards in the hand before turn begins")
                    SettingsField(editableSettings.maxHandSize, {editableSettings = editableSettings.copy(maxHandSize = it)}, isHost, "Max Hand Size", "Maximum number of cards in the hand before turn ends")
                    SettingsField(editableSettings.goldCardPenalty, {editableSettings = editableSettings.copy(goldCardPenalty = it)}, isHost, "Gold Card Penalty", "Points penalty for having a gold card in the hand after game ends")
                    SettingsField(editableSettings.rainbowSheepPoints, {editableSettings = editableSettings.copy(rainbowSheepPoints = it)}, isHost, "Rainbow Sheep Points", "Points for a full rainbow sheep")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    if (isHost && state.opponents.isNotEmpty()) {
                        Button(onStart) {
                            Text("START GAME")
                        }
                    }
                    Button(onExit) {
                        Text("EXIT")
                    }
                }
            }
        }

    }
}

@Composable
private fun SettingsField(
    value: Int,
    onValueChanged: (Int) -> Unit,
    isHost: Boolean,
    label: String,
    details: String
) {
    val state = rememberTextFieldState(value.toString())

    LaunchedEffect(state.text) {
        state.text.toString().toIntOrNull()?.let { parsedInt ->
            if (parsedInt != value) onValueChanged(parsedInt)
        }
    }

    LaunchedEffect(value) {
        val currentStr = state.text.toString()
        if (currentStr.toIntOrNull() != value) {
            state.edit { replace(0, length, value.toString()) }
        }
    }

    val isError = state.text.isEmpty()

    OutlinedTextField(
        state = state,
        readOnly = !isHost,
        isError = isError,
        label = { Text(label) },
        supportingText = {
            if (isError) {
                Text("Value cannot be empty", color = MaterialTheme.colorScheme.error)
            } else {
                Text(details)
            }
        },
        leadingIcon = { Icon(TagIcon, "Number Only") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = {
            val hasNonDigits = toString().any { !it.isDigit() }
            val exceedsMaxLength = length > 9
            if (hasNonDigits || exceedsMaxLength) {
                revertAllChanges()
            }
        }
    )
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun GameBoard(state: GameState, drawerState: DrawerState) {
    if (state.player == null || state.clientRoom == null) return

    val onEvent = LocalOnEvent.current
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(4.dp), Arrangement.spacedBy(4.dp)) {
        ElevatedCard {
            Row(Modifier.fillMaxWidth().padding(8.dp), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
                GameTimer(state.localAnchorTime)
                Spacer(Modifier.weight(1f))
                FilledTonalIconButton({ scope.launch { drawerState.open() } }) { Icon(ChatIcon, "Chat") }
                FilledIconButton(
                    { onEvent(GameScreenEvents.OpenDialog(GameDialogs.ExitConfirmation)) },
                    colors = IconButtonDefaults.iconButtonColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer),
                ) { Icon(ExitIcon, "Exit") }
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f), Arrangement.spacedBy(4.dp)) {
            state.opponents.forEach { opponent ->
                val isTurn = opponent.info.id == state.currentTurnPlayer
                Card(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.secondaryContainer, CardDefaults.shape)
                        .pulsatingBorder(isTurn, MaterialTheme.colorScheme.onSecondaryContainer, CardDefaults.shape, 8.dp, 4.dp)
                        .dropZone(CardDefaults.shape, MaterialTheme.colorScheme.onSecondaryContainer, predicate = { cards ->
                            cards.size == 1 && cards.first().let {
                                (it is Card.SpecialCard && it.specialType == SpecialType.Yoink) || it is Card.GoldCard
                            }
                        }) { droppedCards ->
                            if (droppedCards.size != 1) return@dropZone
                            val droppedCard = droppedCards.first()
                            if (droppedCard is Card.SpecialCard && droppedCard.specialType == SpecialType.Yoink) {
                                onEvent(GameScreenEvents.Yoink(opponent.info.id, droppedCard.id))
                            } else if (droppedCard is Card.GoldCard) {
                                onEvent(GameScreenEvents.RequestCoinFlip(droppedCard.id, opponent.info.id))
                            }
                        },
                    colors = CardDefaults.cardColors(Color.Transparent, MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    OpponentInfo(opponent, ActionAgainstOpponent.getFromHand(state.player.hand), state.currentTurnPlayer == state.player.info.id)
                    FlexBox(Modifier.fillMaxWidth().padding(8.dp).verticalScroll(rememberScrollState()), {
                        gap(4.dp)
                        wrap(FlexWrap.Wrap)
                        alignItems(FlexAlignItems.End)
                        justifyContent(FlexJustifyContent.Center)
                    }) {
                        opponent.info.flock.sheep.forEach { sheep ->
                            key(sheep.id) {
                                DrawSheep(
                                    SheepState.getFromSheepAndHand(sheep, state.player.hand, opponent.info.flock.isWolfProtected, opponent.info.flock.isWheatProtected),
                                    opponent.info.id,
                                    true,
                                    state.currentTurnPlayer == state.player.info.id,
                                    Modifier.dropZone(hoverColor = MaterialTheme.colorScheme.tertiaryContainer, predicate = { cards ->
                                        if (cards.size != 1) return@dropZone false
                                        val card = cards.first()

                                        val sheepState =
                                            SheepState.getFromSheepAndHand(sheep, listOf(card), opponent.info.flock.isWolfProtected, opponent.info.flock.isWheatProtected)

                                        sheepState.deFrankenCandidates.isNotEmpty() ||
                                                sheepState.dePaintCandidates.isNotEmpty() ||
                                                sheepState.deRainbowCandidates.isNotEmpty() ||
                                                sheepState.wolfCandidate != null ||
                                                sheepState.wheatCandidate != null
                                    }) { droppedCards ->
                                        if (droppedCards.size != 1) return@dropZone
                                        val droppedCard = droppedCards.first()
                                        val sheepState =
                                            SheepState.getFromSheepAndHand(sheep, listOf(droppedCard), opponent.info.flock.isWolfProtected, opponent.info.flock.isWheatProtected)
                                        sheepState.deFrankenCandidates.firstOrNull()?.let {
                                            onEvent(GameScreenEvents.FixSheep(FixSheepType.Franken, sheep, droppedCard.id, opponent.info.id))
                                            return@dropZone
                                        }
                                        sheepState.dePaintCandidates.firstOrNull()?.let {
                                            onEvent(GameScreenEvents.FixSheep(FixSheepType.Paint, sheep, droppedCard.id, opponent.info.id))
                                            return@dropZone
                                        }
                                        sheepState.deRainbowCandidates.firstOrNull()?.let {
                                            onEvent(GameScreenEvents.FixSheep(FixSheepType.Rainbow, sheep, droppedCard.id, opponent.info.id))
                                            return@dropZone
                                        }
                                        sheepState.wolfCandidate?.let {
                                            onEvent(GameScreenEvents.Wolf(sheep, opponent.info.id, droppedCard.id))
                                            return@dropZone
                                        }
                                        sheepState.wheatCandidate?.let {
                                            onEvent(GameScreenEvents.Wheat(sheep, opponent.info.id, droppedCard.id))
                                            return@dropZone
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        val isTurn = state.player.info.id == state.currentTurnPlayer
        Card(
            modifier = Modifier.dropZone(CardDefaults.shape, MaterialTheme.colorScheme.onPrimaryContainer, { cards ->
                cards.size > 1 && cards.none { it is Card.SpecialCard || it is Card.GoldCard }
            }) { cards ->
                if (GameLogic.isValidSheep(cards.toList())) onEvent(GameScreenEvents.PlayCards(cards.map { it.id }))
            },
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
        ) {
            Column(Modifier.padding(8.dp), Arrangement.spacedBy(4.dp)) {
                Row(Modifier, Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                    Text("${state.player.info.name} Flock", style = MaterialTheme.typography.titleLargeEmphasized)
                    AnimatedVisibility(state.player.info.flock.isWolfProtected) { Image(painterResource(Res.drawable.wolf), "Wolf Protected", Modifier.size(32.dp)) }
                    AnimatedVisibility(state.player.info.flock.isWheatProtected) { Image(painterResource(Res.drawable.wheat), "Wheat Protected", Modifier.size(32.dp)) }
                }
                LazyRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    items(state.player.info.flock.sheep, { it.id }) { sheep ->
                        DrawSheep(
                            SheepState.getFromSheepAndHand(sheep, state.player.hand, wolfProtected = true, wheatProtected = true)
                                .copy(wheatCandidate = null, wolfCandidate = null),
                            state.player.info.id,
                            true,
                            state.currentTurnPlayer == state.player.info.id,
                            Modifier.animateItem().dropZone(hoverColor = MaterialTheme.colorScheme.tertiaryContainer, predicate = { cards ->
                                cards.size == 1 && cards.first().let {
                                    it is Card.SheepCard && (sheep.deFrankenCandidates(listOf(it)).isNotEmpty() || sheep.dePaintCandidates(listOf(it))
                                        .isNotEmpty() || sheep.deRainbowCandidate(listOf(it)).isNotEmpty())
                                }
                            }) { droppedCards ->
                                if (droppedCards.size != 1) return@dropZone
                                val droppedCard = droppedCards.first()
                                val sheepState = SheepState.getFromSheepAndHand(sheep, listOf(droppedCard), wolfProtected = true, wheatProtected = true)
                                sheepState.deFrankenCandidates.firstOrNull()?.let {
                                    onEvent(GameScreenEvents.FixSheep(FixSheepType.Franken, sheep, droppedCard.id, state.player.info.id))
                                    return@dropZone
                                }
                                sheepState.dePaintCandidates.firstOrNull()?.let {
                                    onEvent(GameScreenEvents.FixSheep(FixSheepType.Paint, sheep, droppedCard.id, state.player.info.id))
                                    return@dropZone
                                }
                                sheepState.deRainbowCandidates.firstOrNull()?.let {
                                    onEvent(GameScreenEvents.FixSheep(FixSheepType.Rainbow, sheep, droppedCard.id, state.player.info.id))
                                    return@dropZone
                                }
                            }
                        )
                    }
                }

            }
        }

        Card(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.tertiaryContainer, CardDefaults.shape)
                .pulsatingBorder(isTurn, MaterialTheme.colorScheme.onTertiaryContainer, CardDefaults.shape, 8.dp, 4.dp),
            colors = CardDefaults.cardColors(Color.Transparent, MaterialTheme.colorScheme.onTertiaryContainer),
        ) {
            Column(Modifier.padding(8.dp), Arrangement.spacedBy(4.dp)) {
                Row(Modifier, Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                    SmallListItem(
                        headline = { Text("Deck") },
                        trailingIcon = {
                            Row {
                                Icon(PlayingCardsIcon, contentDescription = "Deck cards count")
                                Text("${state.clientRoom.deck}")
                            }
                        },
                    )
                    SmallListItem(
                        headline = { Text("Discard Pile") },
                        trailingIcon = {
                            Row {
                                Icon(PlayingCardsIcon, contentDescription = "Discard Pile cards count")
                                Text("${state.clientRoom.discardPile}")
                            }
                        },
                    )
                    AnimatedVisibility(state.player.info.id == state.currentTurnPlayer) {
                        ElevatedButton({
                            onEvent(GameScreenEvents.PlayCards())
                        }) {
                            Text("Play Cards")
                        }
                    }
                    AnimatedVisibility(state.player.info.id == state.currentTurnPlayer) {
                        ElevatedButton(
                            { onEvent(GameScreenEvents.EndTurn) },
                            colors = ButtonDefaults.elevatedButtonColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer),
                        ) {
                            Text("End Turn")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    FilledIconToggleButton(state.sortedHand, { onEvent(GameScreenEvents.SortHand) }) { Icon(SortIcon, "Sort Hand") }
                    FilledIconButton(
                        {
                            val selectedCards = state.player.hand.filter { it.id in state.selectedCards }
                            onEvent(GameScreenEvents.OpenDialog(GameDialogs.DiscardConfirmation(selectedCards)))
                        },
                        Modifier.dropZone(IconButtonDefaults.smallRoundShape, MaterialTheme.colorScheme.onErrorContainer, { true }) {
                            onEvent(GameScreenEvents.OpenDialog(GameDialogs.DiscardConfirmation(it.toList())))
                        },
                        colors = IconButtonDefaults.iconButtonColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer),
                    ) {
                        Icon(DeleteIcon, "Discard Card")
                    }
                }
                LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    val hand = if (state.sortedHand) state.player.hand.sortedWith(
                        compareBy<Card> { it.rank() }.thenBy {
                            when (it) {
                                is Card.GoldCard     -> it.goldCardType
                                is Card.ModifierCard -> it.modifierType
                                is Card.SheepCard    -> it.sheepColor
                                is Card.SpecialCard  -> it.specialType
                            }
                        }.thenBy { if (it is Card.SheepCard) it.sheepSide else null }
                    ) else state.player.hand
                    items(hand, { it.id }) { card ->
                        val isSelected = state.selectedCards.contains(card.id)
                        CardDisplay(
                            card,
                            Modifier.draggableCard(card, false) {
                                state.player.hand.filter { it.id in state.selectedCards }
                            }.animateItem(),
                            isClickable = true,
                            isSelected = isSelected,
                        ) {
                            onEvent(GameScreenEvents.ToggleCard(it.id))
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun ExitConfirmationDialog(exit: () -> Unit) {
    val onEvent = LocalOnEvent.current
    AlertDialog(
        onDismissRequest = {
            onEvent(GameScreenEvents.CloseDialog)
        },
        confirmButton = {
            Button(onClick = {
                onEvent(GameScreenEvents.CloseDialog)
                onEvent(GameScreenEvents.Leave)
                exit()
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            OutlinedButton({
                onEvent(GameScreenEvents.CloseDialog)
            }) {
                Text("Cancel")
            }
        },
        title = { Text("Exit Confirmation") },
        text = {

        },
    )
}

@Composable
fun ExpandedViewDialog(dialog: GameDialogs.ExpandedView) {
    val onEvent = LocalOnEvent.current
    AlertDialog(
        onDismissRequest = {
            onEvent(GameScreenEvents.CloseDialog)
        },
        confirmButton = {
            Button(onClick = { onEvent(GameScreenEvents.CloseDialog) }) { Text("Close") }
        },
        title = { Text(dialog.card?.accessibilityName ?: dialog.sheep?.name ?: "Expanded View") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                dialog.card?.let{
                    CardDisplay(it, hasMenu = false, cardSize = CardSize.Giant)
                }
                dialog.sheep?.let{
                    DrawSheep(SheepState(it),-1, isClickable = false, hasMenu = false, cardSize = CardSize.Giant)
                }
            }

        },
    )
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
fun SelectOpponentSheepDialog(state: GameState, dialog: GameDialogs.SelectOpponentSheep) {
    val onEvent = LocalOnEvent.current
    var selectedSheep by remember { mutableStateOf<Pair<Long, Sheep>?>(null) }

    AlertDialog(
        onDismissRequest = {
            onEvent(GameScreenEvents.CloseDialog)
        },
        confirmButton = {
            Button(enabled = selectedSheep != null, onClick = {
                selectedSheep?.let {
                    if (dialog.card is Card.SpecialCard) {
                        if (dialog.card.specialType == SpecialType.Wheat) {
                            onEvent(GameScreenEvents.CloseDialog)
                            onEvent(GameScreenEvents.Wheat(it.second, it.first, dialog.card.id))
                        } else if (dialog.card.specialType == SpecialType.Wolf) {
                            onEvent(GameScreenEvents.CloseDialog)
                            onEvent(GameScreenEvents.Wolf(it.second, it.first, dialog.card.id))
                        }
                    }
                }
            }) {
                Text("Confirm")
            }
        },
        title = { Text("Select Sheep") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), Arrangement.spacedBy(4.dp), Alignment.CenterHorizontally) {
                CardDisplay(dialog.card)
                state.opponents.forEach { opponent ->
                    Text(opponent.info.name)
                    FlexBox(Modifier.fillMaxWidth(), {
                        gap(4.dp)
                        alignItems(FlexAlignItems.End)
                        justifyContent(FlexJustifyContent.Center)
                        wrap(FlexWrap.Wrap)
                    }) {
                        opponent.info.flock.sheep.forEach { sheep ->
                            DrawSelectableSheep(sheep, sheep == selectedSheep?.second, null, false) { s, _ ->
                                selectedSheep = if (opponent.info.id to s == selectedSheep) null else opponent.info.id to s
                            }
                        }
                    }
                }
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

@Composable
fun SelectOpponentDialog(state: GameState, dialog: GameDialogs.SelectOpponent) {
    val onEvent = LocalOnEvent.current
    var selected by remember { mutableLongStateOf(-1) }

    AlertDialog(
        onDismissRequest = { onEvent(GameScreenEvents.CloseDialog) },
        confirmButton = {
            Button(enabled = selected != -1L, onClick = {
                if (dialog.card is Card.SpecialCard && dialog.card.specialType == SpecialType.Yoink) {
                    onEvent(GameScreenEvents.Yoink(selected, dialog.card.id))
                } else if (dialog.card is Card.GoldCard) {
                    onEvent(GameScreenEvents.RequestCoinFlip(dialog.card.id, selected))
                }
                onEvent(GameScreenEvents.CloseDialog)
            }) { Text("Confirm") }
        },
        title = { Text("Select Opponent") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CardDisplay(dialog.card)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    state.opponents.forEach { opponent ->
                        FilterChip(
                            selected == opponent.info.id,
                            { selected = if (selected == opponent.info.id) -1 else opponent.info.id },
                            { Text(opponent.info.name, style = MaterialTheme.typography.titleLarge) },
                            leadingIcon = {
                                Row {
                                    Icon(PlayingCardsIcon, contentDescription = "${opponent.info.name} cards count")
                                    Text(opponent.numCards.toString())
                                }
                            },
                            trailingIcon = {
                                Row {
                                    if (opponent.info.flock.isWheatProtected) {
                                        Image(painterResource(Res.drawable.wheat), "Wheat Protected", Modifier.size(32.dp))
                                    }
                                    if (opponent.info.flock.isWolfProtected) {
                                        Image(painterResource(Res.drawable.wolf), "Wolf Protected", Modifier.size(32.dp))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun GameOverDialog(state: GameState, dialog: GameDialogs.GameOver) {
    val onEvent = LocalOnEvent.current
    if (state.player == null) return

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            ElevatedButton(onClick = {
                onEvent(GameScreenEvents.ResetGameScreen)
            }) {
                Text("OK")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Game Over", style = MaterialTheme.typography.displayLargeEmphasized)
                Text("Standings", style = MaterialTheme.typography.titleLargeEmphasized)
                Scorecard(dialog.points)
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

@Composable
@OptIn(ExperimentalGridApi::class)
private fun Scorecard(points: List<Pair<String, Int>>) {
    ElevatedCard {
        Grid({
            repeat(3) { column(GridTrackSize.Auto) }
            gap(4.dp, 12.dp)
            repeat(points.size) { row(GridTrackSize.Auto) }
        }, Modifier.padding(8.dp)) {
            points.forEachIndexed { i, (name, point) ->
                val style = if (i == 0) {
                    MaterialTheme.typography.bodyLargeEmphasized.copy(color = MaterialTheme.colorScheme.primary)
                } else {
                    MaterialTheme.typography.bodySmallEmphasized
                }
                Text("${i + 1}", style = style)
                Text(name, Modifier.gridItem(alignment = Alignment.CenterStart), style = style)
                Text("$point", style = style)
            }
        }
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
                if (selectedCards.isNotEmpty() && !GameLogic.isValidSheep(selectedCards)) return@ElevatedButton
                onEvent(GameScreenEvents.CloseDialog)
                onEvent(GameScreenEvents.SubmitSelectedCardsForSheep(selectedCards.map { it.id }))
            }) {
                Text("Confirm")
            }
        }, title = { Text("Select Cards To Make A Valid Sheep") }, text = {
            if(dialog.cards.isEmpty()){
                Text("No Selectable Valid Sheep Card is Present")
            }else{
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
            }
        }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
fun SelectSheepDialog(dialog: GameDialogs.SelectSheep) {
    val onEvent = LocalOnEvent.current
    val selectedSheep = remember { mutableStateSetOf<Pair<Sheep, SheepSide?>>() }

    AlertDialog(
        onDismissRequest = {}, confirmButton = {
            ElevatedButton(enabled = selectedSheep.size <= dialog.amount, onClick = {
                onEvent(GameScreenEvents.CloseDialog)
                onEvent(GameScreenEvents.SubmitSelectedSheep(selectedSheep.toList()))
            }) {
                Text("Confirm")
            }
        }, title = { Text("Select ${dialog.amount} Sheep${if (dialog.selectHalf) " Half" else ""}") }, text = {
            if(dialog.sheep.isEmpty()){
                Text("No Selectable Sheep is Present")
            }else{
                FlexBox(Modifier.fillMaxWidth().padding(8.dp).verticalScroll(rememberScrollState()), {
                    gap(4.dp)
                    alignItems(FlexAlignItems.End)
                    justifyContent(FlexJustifyContent.Center)
                    wrap(FlexWrap.Wrap)
                }) {
                    dialog.sheep.forEach { sheep ->
                        val selected = selectedSheep.firstOrNull { it.first == sheep }
                        DrawSelectableSheep(sheep, selected != null, selected?.second, dialog.selectHalf) { s, side ->
                            selectedSheep.firstOrNull { it.first == s }?.let {
                                selectedSheep.remove(it)
                                if (side != it.second) selectedSheep.add(s to side)
                            } ?: selectedSheep.add(s to side)
                            while (selectedSheep.size > dialog.amount) selectedSheep.remove(selectedSheep.first())
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

    val playerName = state.getName(coinFlip.attacker)
    val opponentName = state.getName(coinFlip.target)

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
    val playerId = state.player!!.info.id
    val currentWinnerId = coinFlip.currentResult?.let { if (it == coinFlip.playerChoice) coinFlip.attacker else coinFlip.target }
    val isWinner = currentWinnerId == playerId
    val hasReFlipCard = state.player.hand.any { it is Card.SpecialCard && it.specialType == SpecialType.ReFlip }
    val canChooseReFlip = coinFlip.currentResult != null &&
            coinFlip.reFlippable > 0 &&
            hasReFlipCard &&
            playerId !in coinFlip.skippedReFlip &&
            playerId !in coinFlip.closedDialog

    LaunchedEffect(coinFlip.iteration, activeComposition) {
        if (coinFlip.currentResult == null) return@LaunchedEffect
        activeComposition?.let{
            isAnimationFinished = false
            animatable.animate(it, initialProgress = 0f)
            isAnimationFinished = true
        }
    }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            if (coinFlip.currentResult != null && isAnimationFinished) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val canClose = !isWinner || coinFlip.reFlippable == 0 || (coinFlip.reFlippable == 1 && hasReFlipCard)
                    ElevatedButton(
                        enabled = canClose,
                        onClick = {
                            onEvent(GameScreenEvents.CloseDialog)
                            if (isWinner) {
                                onEvent(GameScreenEvents.EndCoinFlip)
                            } else {
                                onEvent(GameScreenEvents.CloseFlip)
                            }
                        }
                    ) { Text("Close") }
                    if (canChooseReFlip) {
                        state.player.hand.filterIsInstance<Card.SpecialCard>().firstOrNull { it.specialType == SpecialType.ReFlip }?.let {
                            ElevatedButton(onClick = { onEvent(GameScreenEvents.ReFlip(it.id)) }) { Text("Consume ReFlip Card & Re-Flip") }
                            OutlinedButton(onClick = { onEvent(GameScreenEvents.SkipReFlip) }) { Text("Skip Re-Flip") }
                        }
                    }
                }
            }
        }, title = { Text("Coin Flip") },
        text = {
            if (headsComposition.isLoading || tailsComposition.isLoading) {
                CircularProgressIndicator()
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally) {
                    Row(Modifier, Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                        Text("$playerName Played")
                        CardDisplay(coinFlip.goldCard)
                        Text("Against $opponentName")
                    }
                    coinFlip.lastReFlippedBy?.let {
                        Text("Re flipped By ${state.getName(it)}")
                    }
                    if (coinFlip.playerChoice == null) {
                        if (coinFlip.attacker == playerId) {
                            var isHeads: Boolean? by remember { mutableStateOf(null) }

                            Text("Select Your Choice")
                            Row(Modifier, horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isHeads = true }, Modifier.size(80.dp)) {
                                    Icon(
                                        CoinHeadIcon,
                                        "Heads",
                                        Modifier.fillMaxSize(),
                                        tint = if (isHeads == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                    )
                                }
                                IconButton(onClick = { isHeads = false }, Modifier.size(80.dp)) {
                                    Icon(
                                        CoinTailIcon,
                                        "Tails",
                                        Modifier.fillMaxSize(),
                                        tint = if (isHeads == false) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                    )
                                }
                            }
                            ElevatedButton(enabled = isHeads != null, onClick = {
                                onEvent(GameScreenEvents.SelectCoinFace(isHeads!!))
                            }) {
                                Text("Confirm")
                            }
                        } else {
                            Text("Waiting for $playerName to select face")
                        }
                    } else if (coinFlip.currentResult == null) {
                        Text("$playerName Called ${coinFlip.playerChoice!!.headTail()}")
                        if (coinFlip.target == playerId) {
                            ElevatedButton(onClick = {
                                onEvent(GameScreenEvents.FlipCoin)
                            }) {
                                Text("Flip Coin")
                            }
                        } else {
                            Text("Waiting for $opponentName to flip coin")
                        }
                    } else {
                        Text("$playerName Called ${coinFlip.playerChoice!!.headTail()}, Result Is ${if (isAnimationFinished) coinFlip.currentResult!!.headTail() else "..."}")
                        coinFlip.currentResult?.let { result ->
                            if (activeComposition == null) {
                                CircularProgressIndicator()
                            } else {
                                Image(
                                    painter = rememberLottiePainter(composition = activeComposition, progress = { animatable.progress }),
                                    contentDescription = "Coin Flip Animation",
                                    modifier = Modifier.size(100.dp),
                                    contentScale = ContentScale.Crop,
                                )
                            }

                            if (isAnimationFinished) {
                                Text("${if (result == coinFlip.playerChoice) playerName else opponentName} Wins.")
                            }
                        }
                    }
                }
            }
        }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
fun SelectCardsDialog(dialog: GameDialogs.SelectCards) {
    val onEvent = LocalOnEvent.current
    val selectedCards = remember { mutableStateListOf<Int>() }

    AlertDialog(onDismissRequest = {
        onEvent(GameScreenEvents.CloseDialog)
    }, confirmButton = {
        ElevatedButton(enabled = selectedCards.size == dialog.amount, onClick = {
            if (selectedCards.size != dialog.amount) return@ElevatedButton
            onEvent(GameScreenEvents.CloseDialog)
            onEvent(GameScreenEvents.SubmitSelectedCards(selectedCards.toList(), dialog.cardId, dialog.opponentId))
        }) {
            Text("Confirm")
        }
    }, dismissButton = {
        OutlinedButton(onClick = { onEvent(GameScreenEvents.CloseDialog) }) {
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
                DrawHiddenCard(cardId, cardId in selectedCards) {
                    if (!selectedCards.remove(it)) selectedCards.add(it)
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
            onEvent(GameScreenEvents.CloseDialog)
        },
        confirmButton = {
            ElevatedButton(onClick = { onEvent(GameScreenEvents.CloseDialog) }) {
                Text("OK")
            }
        },
        text = { Text(message) },
    )
}

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
fun DiscardConfirmationDialog(cards: List<Card>) {
    val onEvent = LocalOnEvent.current
    AlertDialog(
        onDismissRequest = {
            onEvent(GameScreenEvents.CloseDialog)
        }, confirmButton = {
            ElevatedButton(onClick = {
                onEvent(GameScreenEvents.CloseDialog)
                onEvent(GameScreenEvents.Discard(cards))
            }) {
                Text("OK")
            }
        }, dismissButton = {
            OutlinedButton(onClick = { onEvent(GameScreenEvents.CloseDialog) }) {
                Text("Cancel")
            }
        }, title = { Text("Confirm Discard") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Are you sure you want to discard these cards?")
                FlexBox(Modifier.padding(8.dp).verticalScroll(rememberScrollState()), {
                    gap(4.dp)
                }) {
                    cards.forEach { CardDisplay(it) }
                }
            }
        }, properties = DialogProperties(usePlatformDefaultWidth = false)
    )
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
        Row(Modifier.padding(8.dp).clickable { isMenuExpanded = !isMenuExpanded }, Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            Text(opponent.info.name, style = MaterialTheme.typography.titleLarge)
            Row {
                Icon(PlayingCardsIcon, contentDescription = "${opponent.info.name} cards count")
                Text(opponent.numCards.toString())
            }
            AnimatedVisibility(opponent.info.flock.isWolfProtected) { Image(painterResource(Res.drawable.wolf), "Wolf Protected", Modifier.size(32.dp)) }
            AnimatedVisibility(opponent.info.flock.isWheatProtected) { Image(painterResource(Res.drawable.wheat), "Wheat Protected", Modifier.size(32.dp)) }
        }

        DropdownMenu(
            expanded = isMenuExpanded && hasMenu, onDismissRequest = { isMenuExpanded = false }) {
            aao.yoink?.let {
                DropdownMenuItem(text = { Text("Yoink 2 Cards") }, onClick = {
                    onEvent(GameScreenEvents.Yoink(opponent.info.id, it))
                })
            }
            aao.goldYoink?.let {
                DropdownMenuItem(text = { Text("Yoink Entire Hand") }, trailingIcon = { Icon(CoinHeadIcon, "Requires Flipping A Coin") }, onClick = {
                    onEvent(GameScreenEvents.RequestCoinFlip(it, opponent.info.id))
                })
            }
            aao.goldLure2Sheep?.let {
                DropdownMenuItem(text = { Text("Lure 2 Sheep") }, trailingIcon = { Icon(CoinHeadIcon, "Requires Flipping A Coin") }, onClick = {
                    onEvent(GameScreenEvents.RequestCoinFlip(it, opponent.info.id))
                })
            }
            aao.goldHalve2Sheep?.let {
                DropdownMenuItem(text = { Text("Halve 2 Sheep") }, trailingIcon = { Icon(CoinHeadIcon, "Requires Flipping A Coin") }, onClick = {
                    onEvent(GameScreenEvents.RequestCoinFlip(it, opponent.info.id))
                })
            }
            aao.goldRemove2Sheep?.let {
                DropdownMenuItem(text = { Text("Remove 2 Sheep") }, trailingIcon = { Icon(CoinHeadIcon, "Requires Flipping A Coin") }, onClick = {
                    onEvent(GameScreenEvents.RequestCoinFlip(it, opponent.info.id))
                })
            }
            aao.goldRecoverSheep?.let {
                DropdownMenuItem(text = { Text("Recover 1 Sheep") }, trailingIcon = { Icon(CoinHeadIcon, "Requires Flipping A Coin") }, onClick = {
                    onEvent(GameScreenEvents.RequestCoinFlip(it, opponent.info.id))
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
    val wolfCandidate: Int? = null,
    val wheatCandidate: Int? = null,
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
private fun DrawSheep(state: SheepState, owner: Long, isClickable: Boolean, hasMenu: Boolean, modifier: Modifier = Modifier, cardSize: CardSize = CardSize.Small) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val onEvent = LocalOnEvent.current

    Box(
        modifier
            .pointerInput(isClickable) {
                if(!isClickable) return@pointerInput
                detectTapGestures(onDoubleTap = {
                    onEvent(GameScreenEvents.OpenDialog(GameDialogs.ExpandedView(sheep = state.sheep)))
                }, onLongPress = { if(hasMenu) isMenuExpanded = true })
            }.pointerInput(isClickable) {
                if(!isClickable) return@pointerInput
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (hasMenu && event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.forEach { it.consume() }
                            isMenuExpanded = true
                        }
                    }
                }
            }
    ) {
        state.sheep.modifier?.let { card ->
            CardDisplay(card, Modifier.align(Alignment.TopCenter), cardSize = cardSize)
        }
        Row(
            Modifier.then(
                if (state.sheep.modifier != null) Modifier.padding(top = cardSize.size.width/3.63f)
                else Modifier
            ),
        ) {
            CardDisplay(state.sheep.head, Modifier.rotate(if (state.sheep.isFrankenButts) 180f else 0f), cardSize = cardSize)
            CardDisplay(state.sheep.butt, Modifier.rotate(if (state.sheep.isFrankenHeads) 180f else 0f), cardSize = cardSize)
        }
        DropdownMenu(expanded = isMenuExpanded && hasMenu, onDismissRequest = { isMenuExpanded = false }) {
            DropdownMenuItem(text = { Text("View") }, onClick = {
                isMenuExpanded = false
                onEvent(GameScreenEvents.OpenDialog(GameDialogs.ExpandedView(sheep = state.sheep)))
            })
            if (state.deFrankenCandidates.isNotEmpty()) HorizontalDivider()
            state.deFrankenCandidates.forEach { candidate ->
                DropdownMenuItem(text = { Text("Fix Franken Using ${candidate.name}") }, onClick = {
                    isMenuExpanded = false
                    onEvent(GameScreenEvents.FixSheep(FixSheepType.Franken, state.sheep, candidate.id, owner))
                })
            }
            if (state.dePaintCandidates.isNotEmpty()) HorizontalDivider()
            state.dePaintCandidates.forEach { candidate ->
                DropdownMenuItem(text = { Text("Fix Paint Using ${candidate.name}") }, onClick = {
                    isMenuExpanded = false
                    onEvent(GameScreenEvents.FixSheep(FixSheepType.Paint, state.sheep, candidate.id, owner))
                })
            }
            if (state.deRainbowCandidates.isNotEmpty()) HorizontalDivider()
            state.deRainbowCandidates.forEach { candidate ->
                DropdownMenuItem(text = { Text("Fix Rainbow Using ${candidate.name}") }, onClick = {
                    isMenuExpanded = false
                    onEvent(GameScreenEvents.FixSheep(FixSheepType.Rainbow, state.sheep, candidate.id, owner))
                })
            }
            state.wolfCandidate?.let {
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Wolf") }, onClick = {
                    isMenuExpanded = false
                    onEvent(GameScreenEvents.Wolf(state.sheep, owner, it))
                })
            }
            state.wheatCandidate?.let {
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Wheat") }, onClick = {
                    isMenuExpanded = false
                    onEvent(GameScreenEvents.Wheat(state.sheep, owner, it))
                })
            }
        }
    }
}

@Composable
private fun DrawSelectableSheep(sheep: Sheep, selected: Boolean, selectedSide: SheepSide?, selectHalf: Boolean, onSelect: (Sheep, SheepSide?) -> Unit) {
    Box(Modifier
        .clickable(!selectHalf) { onSelect(sheep, null) }
        .clip(RoundedCornerShape(4.dp))
        .padding(4.dp)
        .then(if(selected) Modifier.animatedDottedBorder(3.dp, MaterialTheme.colorScheme.tertiaryContainer) else Modifier)
    ) {
        sheep.modifier?.let { card ->
            CardDisplay(card, Modifier.align(Alignment.TopCenter))
        }
        Row(
            Modifier.align(Alignment.Center).then(
                if (sheep.modifier != null) Modifier.padding(top = 22.dp)
                else Modifier
            ),
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

@Composable
@Preview(name = "Light", widthDp = 1920, heightDp = 1080)
@Preview(widthDp = 1920, heightDp = 1080, uiMode = UI_MODE_NIGHT_YES)
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
                Card.GoldCard(20, GoldCardType.Remove),
                Card.GoldCard(21, GoldCardType.Halve),
                Card.GoldCard(22, GoldCardType.Recover),
                Card.GoldCard(23, GoldCardType.Lure),
            )
        ),
        selectedCards = mutableStateListOf(2),
        opponents = mutableStateSetOf(
            Opponent(PlayerInfo(2, "Opponent 1", Flock(listOf(pinkYellowFrankenSheep, brownBeigePaintSheep))), 8),
            Opponent(PlayerInfo(3, "Opponent 2", Flock(listOf(rainbowOrangeSheep, blackWhiteFrankenSheep))), 5),
            Opponent(PlayerInfo(4, "Opponent 3", Flock(listOf(blackWhiteFrankenSheep, pinkYellowFrankenSheep))), 3),
        ),
        previousGameScores = mutableStateListOf(
            listOf(
                "Player 1" to 8,
                "Player 2" to 5,
                "Player 3" to 3,
                "Player 4" to 0,
            ),
            listOf(
                "Player 1" to 8,
                "Player 2" to 5,
                "Player 3" to 3,
                "Player 4" to 0,
            ),
        ),
        clientRoom = ClientRoom(
            code = "123456",
            players = 4,
            host = PlayerInfo(1, "Player", Flock(listOf())),
            deck = 54,
            discardPile = 0,
            isStarted = true,
        ),
        currentTurnPlayer = 1,
        coinFlip = CoinFlip(
            Card.GoldCard(19, GoldCardType.Lure), 1, 2,
            playerChoice = true,
            currentResult = false,
            reFlippable = 1,
            skippedReFlip = emptyList(),
            closedDialog = emptyList(),
            iteration = 0
        ),
        events = listOf(
            GameEvents.TurnChange(1, "player"),
            GameEvents.DrawCards(2, 1, "player"),
            GameEvents.PlaceSheep(3, "Red Front Red Back", "player"),
            GameEvents.YoinkCards(4, 2, "player", "opponent"),
            GameEvents.DiscardedCards(5, 2, "player"),
            GameEvents.TurnChange(6, "player 2"),
            GameEvents.WheatWolf(7, true, "Red Front Red Back", "player", "opponent"),
            GameEvents.WheatWolf(8, false, "Rainbow Front Blue Back", "player", "opponent"),
            GameEvents.FixSheep(9, FixSheepType.Franken, "Green Front Yellow Front Franken", "player", "opponent"),
            GameEvents.FixSheep(10, FixSheepType.Paint, "Green Back Yellow Back Painted", "player", "opponent"),
            GameEvents.FixSheep(11, FixSheepType.Rainbow, "Rainbow Front Blue Back", "player", null),
            GameEvents.PlayGoldCard(12, GoldCardType.Lure, "player", "opponent"),
            GameEvents.CoinFlipResult(13, "player", "opponent", playerChoice = true, result = false),
            GameEvents.ReFlipped(14, "player"),
            GameEvents.CoinFlipResult(15, "player", "opponent", playerChoice = false, result = false),
            GameEvents.GoldCardResult(16, GoldCardType.Lure, 2, "player", "opponent"),
        ).reversed().toMutableStateList(),
    )
    val dialogs = listOf<GameDialogs>(
//        GameDialogs.ExpandedView(sheep = pinkYellowFrankenSheep)
    )

    PreviewContext()
    CardGameTheme {
        Surface {
            CompositionLocalProvider(LocalOnEvent provides {}) {
                if (state.clientRoom?.isStarted == true) GameBoardWrapper(state)
                else PreStartUI(state, {}) {}
                DisplayDialogs(state, dialogs) {}
            }
        }
    }
}
