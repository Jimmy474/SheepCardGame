package com.jimmy.sheepcardgame

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jimmy.sheepcardgame.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock

@KoinViewModel
class GameViewModel : ViewModel() {
    val state: StateFlow<GameState>
        field = MutableStateFlow(GameState())

    val dialogs: StateFlow<List<GameDialogs>>
        field = MutableStateFlow(emptyList())

    private val client = GameClient {
        eventQueue.trySend(it)
    }

    private val eventQueue = Channel<S2CEvent>(Channel.UNLIMITED)

    init {
        startProcessor()
    }

    private fun startProcessor() {
        viewModelScope.launch {
            for (event in eventQueue) {
                try {
                    processEventSequentially(event)
                } catch (e: Exception) {
                    println("Error processing event $event: ${e.message}")
                }
            }
        }
    }

    private fun processEventSequentially(event: S2CEvent) {
        when (event) {
            is S2CEvent.NotifyGameEventS2CEvent           -> state.value.events.add(0,event.event)
            is S2CEvent.SyncScoresS2CEvent                -> {
                state.value.previousGameScores.clear()
                state.value.previousGameScores.addAll(event.scores)
            }
            is S2CEvent.UpdateRoomSettingsS2CEvent        -> state.update { it.copy(settings = event.settings) }
            is S2CEvent.UpdatePlayersS2CEvent             -> {
                state.update {
                    it.copy(player = event.player, currentTurnPlayer = event.activeTurnPlayer)
                }
                state.value.opponents.clear()
                state.value.opponents.addAll(event.opponents)
            }

            is S2CEvent.InitializePlayerS2CEvent          -> state.update { it.copy(player = event.player) }
            is S2CEvent.InitializeOpponentsS2CEvent       -> {
                state.value.opponents.clear()
                state.value.opponents.addAll(event.opponents)
            }
            is S2CEvent.OpponentJoinedS2CEvent            -> state.value.opponents += event.opponent
            is S2CEvent.OpponentLeftS2CEvent              -> state.value.opponents -= event.opponent
            is S2CEvent.SelectFromGivenCardsS2CEvent      -> onEvent(state,
                GameScreenEvents.OpenDialog(
                    GameDialogs.SelectCards(
                        event.amount, event.cards, event.cardId, event.opponentId
                    )
                )
            )

            is S2CEvent.SelectFromGivenSheepS2CEvent      -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.SelectSheep(event.amount, event.sheep, event.selectHalf)))
            is S2CEvent.SelectSheepFromGivenCardsS2CEvent -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.SelectCardsForSheep(event.cards)))
            is S2CEvent.UpdateClientRoomS2CEvent          -> state.update { it.copy(clientRoom = event.clientRoom, localAnchorTime = Clock.System.now().toEpochMilliseconds() - event.clientRoom.elapsedTime) }
            is S2CEvent.ExceedsMaxHandSizeS2CEvent        -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.Info("You have ${event.extraCards} extra cards in your hand, either discard them or use them to continue. If you fail to do so before the timer ends random cards from your hand will be discarded instead to end your turn.")))

            is S2CEvent.CoinFlipInitiateS2CEvent          -> {
                state.update { it.copy(coinFlip = event.coinFlip) }
                onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.CoinFlip))
            }

            is S2CEvent.UpdateCoinFlipS2CEvent            -> state.update { it.copy(coinFlip = event.coinFlip) }

            is S2CEvent.CloseCoinFlipS2CEvent             -> {
                state.update { it.copy(coinFlip = null) }
                dialogs.update { gameDialogs -> gameDialogs.filterNot { it is GameDialogs.CoinFlip } }
            }

            is S2CEvent.NotificationS2CEvent              -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.Info(event.message)))
            S2CEvent.LastTurnS2CEvent                     -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.Info("This is your last turn, make sure you play all your cards.")))
            S2CEvent.FinalRoundS2CEvent                   -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.Info("The Final Round Begins now. Each player get 1 last turn before the game ends.")))
            is S2CEvent.GameOverS2CEvent                  -> {
                state.value.previousGameScores.add(event.points)
                onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.GameOver(event.points)))
            }
        }
    }

    fun onEventFixed(event: GameScreenEvents) = onEvent(state,event)

    fun onEvent(state: MutableStateFlow<GameState>,event: GameScreenEvents): Job {
        return viewModelScope.launch {
            when (event) {
                GameScreenEvents.SortHand -> state.update { it.copy(sortedHand = !it.sortedHand) }

                is GameScreenEvents.Leave                        -> {
                    client.sendEventToServer(C2SEvent.LeaveMidGameC2SEvent(state.value.player!!.info.id))
                    disconnectFromServer()
                }

                is GameScreenEvents.SaveRoomSettings            -> {
                    client.sendEventToServer(
                        C2SEvent.RequestRoomSettingsUpdateC2SEvent(
                            event.settings, state.value.player!!.info.id
                        )
                    )
                }

                is GameScreenEvents.OpenDialog -> {
                    dialogs.update{ it + event.dialog }
                }

                GameScreenEvents.CloseDialog -> {
                    dialogs.update { it.drop(1) }
                }

                is GameScreenEvents.PlayCards  -> {
                    state.value.let {
                        val selectedCards = event.cards.takeIf { c -> c.isNotEmpty() } ?: it.selectedCards.toList()
                        if (it.player == null || it.currentTurnPlayer == -1L || selectedCards.isEmpty()) return@launch

                        if (it.player.info.id != it.currentTurnPlayer){
                            onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.Info("Wait For Your Turn")))
                            return@launch
                        }

                        if (selectedCards.size == 1) {
                            val card = it.player.hand.first { card -> card.id == selectedCards.first() }
                            when (card) {
                                is Card.SheepCard    -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.Info("Selected Cards do not make a valid sheep.")))
                                is Card.SpecialCard  -> when (card.specialType) {
                                    SpecialType.ReFlip -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.Info("Re Flip can not be played outside of a coin flip.")))
                                    SpecialType.Wheat  -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.SelectOpponentSheep(card)))
                                    SpecialType.Wolf   -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.SelectOpponentSheep(card)))
                                    SpecialType.Yoink  -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.SelectOpponent(card)))
                                }

                                is Card.ModifierCard -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.Info("Modifier Cards can only be played with Sheep Cards.")))
                                is Card.GoldCard     -> onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.SelectOpponent(card)))
                            }
                            return@launch
                        }

                        if (selectedCards.size > 3) {
                            onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.Info("You must select at most 3 cards to play")))
                            return@launch
                        }

                        val cards = it.player.hand.filter { card -> card.id in selectedCards }
                        if (!GameLogic.isValidSheep(cards)) {
                            onEvent(state,GameScreenEvents.OpenDialog(GameDialogs.Info("Selected Cards do not make a valid sheep")))
                            return@launch
                        }

                        client.sendEventToServer(C2SEvent.PlayCardsC2SEvent(selectedCards, it.player.info.id))
                    }

                    state.value.selectedCards.clear()
                }

                is GameScreenEvents.ToggleCard                  -> {
                    if(!state.value.selectedCards.remove(event.cardId)) state.value.selectedCards.add(event.cardId)
                    state.value.selectedCards.removeAll { id ->
                        state.value.player!!.hand.none { it.id == id }
                    }
                }

                GameScreenEvents.EndTurn                        -> {
                    client.sendEventToServer(C2SEvent.EndTurnC2SEvent(state.value.player!!.info.id))
                }

                is GameScreenEvents.Wolf                        -> {
                    client.sendEventToServer(
                        C2SEvent.WolfC2SEvent(
                            event.sheep, event.cardId, event.owner, state.value.player!!.info.id
                        )
                    )
                }

                is GameScreenEvents.Wheat                       -> {
                    client.sendEventToServer(
                        C2SEvent.WheatC2SEvent(
                            event.sheep, event.cardId, event.owner, state.value.player!!.info.id
                        )
                    )
                }

                is GameScreenEvents.Yoink                       -> {
                    client.sendEventToServer(
                        C2SEvent.RequestCardSelectionC2SEvent(
                            event.opponent, event.cardId, state.value.player!!.info.id
                        )
                    )
                }

                is GameScreenEvents.SubmitSelectedCards         -> {
                    client.sendEventToServer(
                        C2SEvent.SelectedCardsC2SEvent(
                            event.cards, event.cardId, event.opponentId, state.value.player!!.info.id
                        )
                    )
                }

                is GameScreenEvents.SubmitSelectedSheep         -> {
                    client.sendEventToServer(C2SEvent.SelectedSheepC2SEvent(event.sheep, state.value.player!!.info.id))
                }
                is GameScreenEvents.SubmitSelectedCardsForSheep -> {
                    client.sendEventToServer(C2SEvent.SelectedCardsForSheepC2SEvent(event.cards, state.value.player!!.info.id))
                }

                is GameScreenEvents.RequestCoinFlip             -> {
                    client.sendEventToServer(C2SEvent.RequestCoinFlipC2SEvent(event.cardId, event.opponentId, state.value.player!!.info.id))
                }

                is GameScreenEvents.SelectCoinFace              -> {
                    client.sendEventToServer(C2SEvent.SelectFaceCoinFlipC2SEvent(event.isHead,state.value.player!!.info.id))
                }
                GameScreenEvents.FlipCoin                       -> {
                    client.sendEventToServer(C2SEvent.FlipCoinC2SEvent(state.value.player!!.info.id))
                }
                is GameScreenEvents.ReFlip                      -> {
                    client.sendEventToServer(C2SEvent.ReFlipCoinC2SEvent(event.cardId, state.value.player!!.info.id))
                }
                GameScreenEvents.CloseFlip                      -> {
                    client.sendEventToServer(C2SEvent.SkipReFlipCoinC2SEvent(true, state.value.player!!.info.id))
                }
                GameScreenEvents.SkipReFlip                     -> {
                    client.sendEventToServer(C2SEvent.SkipReFlipCoinC2SEvent(false, state.value.player!!.info.id))
                }
                GameScreenEvents.EndCoinFlip                    -> {
                    client.sendEventToServer(C2SEvent.EndCoinFlipC2SEvent(state.value.player!!.info.id))
                }

                is GameScreenEvents.FixSheep                    -> {
                    client.sendEventToServer(
                        C2SEvent.FixSheepC2SEvent(
                            event.fixType, event.sheep, event.cardId, event.owner, state.value.player!!.info.id
                        )
                    )
                }

                is GameScreenEvents.Discard                     -> {
                    client.sendEventToServer(C2SEvent.DiscardC2SEvent(event.cards, state.value.player!!.info.id))
                }

                GameScreenEvents.ResetGameScreen                -> {
                    state.update {
                        it.copy(
                            currentTurnPlayer = -1,
                            coinFlip = null,
                        )
                    }
                    state.value.selectedCards.clear()
                    dialogs.update { emptyList() }
                    state.value.roomsToJoin.clear()
                    state.value.events.clear()
                }
            }
        }
    }

    fun fetchRoomsList() {
        viewModelScope.launch {
            val rooms = client.getRoomsList()
            state.value.roomsToJoin.addAll(rooms)
        }
    }

    fun connectToServerCreateRoom(text: String, navigateToGame: () -> Unit, failedCallback: () -> Unit) {
        viewModelScope.launch {
            client.connect(text, "create", navigateToGame, failedCallback)
        }
    }

    fun connectToServerJoinRoom(text: String, code: String, navigateToGame: () -> Unit, failedCallback: () -> Unit) {
        viewModelScope.launch {
            client.connect(text, "join", navigateToGame, failedCallback, code)
        }
    }

    fun disconnectFromServer() {
        viewModelScope.launch {
            client.disconnect()
            state.update { GameState() }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            client.sendEventToServer(C2SEvent.StartGameC2SEvent)
        }
    }
}

data class GameState(
    val player: Player? = null,
    val selectedCards: SnapshotStateList<Int> = mutableStateListOf(),
    val opponents: SnapshotStateSet<Opponent> = mutableStateSetOf(),
    val clientRoom: ClientRoom? = null,
    val previousGameScores: SnapshotStateList<List<Pair<String, Int>>> = mutableStateListOf(),
    val roomsToJoin: SnapshotStateSet<ClientRoom> = mutableStateSetOf(),
    val currentTurnPlayer: Long = -1,
    val coinFlip: CoinFlip? = null,
    val settings: RoomSettings = RoomSettings(),
    val events: SnapshotStateList<GameEvents> = mutableStateListOf(),
    val localAnchorTime: Long = 0L,
    val sortedHand: Boolean = false,
){
    fun getName(id: Long): String{
        player?.let{
            if(it.info.id == id) return it.info.name
        }
        opponents.forEach {
            if(it.info.id == id) return it.info.name
        }
        return "Unknown"
    }
}

sealed interface GameScreenEvents {
    data class OpenDialog(val dialog: GameDialogs) : GameScreenEvents
    data object CloseDialog: GameScreenEvents
    data object SortHand: GameScreenEvents
    data class ToggleCard(val cardId: Int) : GameScreenEvents
    data class PlayCards(val cards: List<Int> = emptyList()) : GameScreenEvents
    data class Discard(val cards: List<Card>) : GameScreenEvents
    data object EndTurn : GameScreenEvents

    data class Wolf(val sheep: Sheep, val owner: Long, val cardId: Int) : GameScreenEvents
    data class Wheat(val sheep: Sheep, val owner: Long, val cardId: Int) : GameScreenEvents
    data class Yoink(val opponent: Long, val cardId: Int) : GameScreenEvents
    data class RequestCoinFlip(val cardId: Int, val opponentId: Long) : GameScreenEvents
    data class SubmitSelectedCards(val cards: List<Int>, val cardId: Int, val opponentId: Long) : GameScreenEvents
    data class SubmitSelectedSheep(val sheep: List<Pair<Sheep, SheepSide?>>) : GameScreenEvents
    data class SubmitSelectedCardsForSheep(val cards: List<Int>) : GameScreenEvents

    data class FixSheep(val fixType: FixSheepType, val sheep: Sheep, val cardId: Int, val owner: Long) : GameScreenEvents

    data class SelectCoinFace(val isHead: Boolean) : GameScreenEvents
    data object FlipCoin : GameScreenEvents
    data class ReFlip(val cardId: Int) : GameScreenEvents
    data object SkipReFlip : GameScreenEvents
    data object CloseFlip : GameScreenEvents
    data object EndCoinFlip : GameScreenEvents

    data object ResetGameScreen : GameScreenEvents
    data object Leave : GameScreenEvents

    data class SaveRoomSettings(val settings: RoomSettings) : GameScreenEvents
}

@Serializable
sealed interface GameDialogs {
    @Serializable
    data class DiscardConfirmation(val cards: List<Card>) : GameDialogs
    @Serializable
    data class Info(val message: String) : GameDialogs
    @Serializable
    data class SelectCards(val amount: Int, val cards: List<Int>, val cardId: Int, val opponentId: Long) : GameDialogs
    @Serializable
    data class SelectSheep(val amount: Int, val sheep: List<Sheep>, val selectHalf: Boolean) : GameDialogs
    @Serializable
    data class SelectCardsForSheep(val cards: List<Card>) : GameDialogs
    @Serializable
    data class SelectOpponent(val card: Card) : GameDialogs
    @Serializable
    data class SelectOpponentSheep(val card: Card) : GameDialogs
    @Serializable
    data class ExpandedView(val card: Card? = null, val sheep: Sheep? = null) : GameDialogs
    @Serializable
    data object CoinFlip : GameDialogs
    @Serializable
    data object ExitConfirmation : GameDialogs
    @Serializable
    data object FinalRound : GameDialogs
    @Serializable
    data class GameOver(val points: List<Pair<String, Int>>) : GameDialogs

}