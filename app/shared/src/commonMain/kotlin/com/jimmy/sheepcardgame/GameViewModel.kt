package com.jimmy.sheepcardgame

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

@KoinViewModel
class GameViewModel : ViewModel() {
    val state: StateFlow<GameState>
        field = MutableStateFlow(GameState())

    private val client = GameClient {
        eventQueue.trySend(it)
    }

    private val eventQueue = Channel<S2CEvent>(Channel.UNLIMITED)
    private val pendingInput = MutableStateFlow(PendingInput())

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
            is S2CEvent.UpdatePlayersS2CEvent        -> {
                state.update { it.copy(player = event.player, opponents = event.opponents, currentTurnPlayer = event.activeTurnPlayer) }
            }

            is S2CEvent.InitializePlayerS2CEvent     -> {
                state.update { it.copy(player = event.player) }
            }

            is S2CEvent.InitializeOpponentsS2CEvent  -> {
                state.update { it.copy(opponents = event.opponents) }
            }

            is S2CEvent.OpponentJoinedS2C            -> {
                state.update { it.copy(opponents = it.opponents + event.opponent) }
            }

            is S2CEvent.OpponentLeftS2C              -> {
                state.update { it.copy(opponents = it.opponents - event.opponent) }
            }

            is S2CEvent.SelectFromGivenCardsS2CEvent -> onEvent(GameEvents.ChangeDialog(GameDialogs.SelectCards(event.amount, event.cards, event.cardId, event.opponentId)))
            is S2CEvent.SelectFromGivenSheepS2CEvent -> onEvent(GameEvents.ChangeDialog(GameDialogs.SelectSheep(event.amount, event.sheep, event.selectHalf)))
            is S2CEvent.SelectSheepFromGivenCardsS2CEvent -> onEvent(GameEvents.ChangeDialog(GameDialogs.SelectCardsForSheep(event.cards)))

            is S2CEvent.UpdateClientRoomS2CEvent     -> {
                state.update { it.copy(clientRoom = event.clientRoom) }
            }

            is S2CEvent.ExceedsMaxHandSizeS2CEvent   -> {
                onEvent(GameEvents.ChangeDialog(GameDialogs.ExceedsMaxHandSize(event.extraCards)))
            }

            is S2CEvent.CoinFlipInitiateS2CEvent -> {
                state.update { it.copy(coinFlip = event.coinFlip) }
                if(state.value.dialog is GameDialogs.CoinFlip) return
                onEvent(GameEvents.ChangeDialog(GameDialogs.CoinFlip))
            }

            is S2CEvent.CloseCoinFlipS2CEvent -> {
                state.update { it.copy(coinFlip = null) }
                if(state.value.dialog is GameDialogs.CoinFlip) onEvent(GameEvents.ChangeDialog(GameDialogs.None))
            }

            is S2CEvent.NotificationS2CEvent -> onEvent(GameEvents.ChangeDialog(GameDialogs.Info(event.message)))
        }
    }

    fun onEvent(event: GameEvents): Job {
        return viewModelScope.launch {
            when (event) {
                is GameEvents.ChangeDialog        -> {
                    if(event.dialog is GameDialogs.SelectCoinFace && state.value.player?.hand?.none { it.id == event.dialog.cardId } == true) return@launch
                    state.update { it.copy(dialog = event.dialog) }
                }

                GameEvents.PlayCards              -> {
                    state.value.let {
                        if (it.player == null || it.currentTurnPlayer == -1L || it.player.info.id != it.currentTurnPlayer || it.selectedCards.isEmpty()) return@launch
                        if (it.selectedCards.size < 2) {
                            onEvent(GameEvents.ChangeDialog(GameDialogs.Info("You must select at least 2 cards to play")))
                            return@launch
                        }
                        if (it.selectedCards.size > 3) {
                            onEvent(GameEvents.ChangeDialog(GameDialogs.Info("You must select at most 3 cards to play")))
                            return@launch
                        }

                        val cards = it.player.hand.filter { card -> card.id in it.selectedCards }
                        if (!GameLogic.isValidSheep(cards)) {
                            onEvent(GameEvents.ChangeDialog(GameDialogs.Info("Selected Cards do not make a valid sheep")))
                            return@launch
                        }

                        sendC2SEvent(C2SEvent.PlayCardsC2SEvent(it.selectedCards, it.player.info.id))
                    }

                    state.update {
                        it.copy(selectedCards = emptyList())
                    }
                }

                is GameEvents.ToggleCard          -> state.update { gameState ->
                    val selected = gameState.selectedCards
                        .filter { id -> gameState.player!!.hand.any { it.id == id } }
                        .toMutableList()

                    if (!selected.remove(event.cardId)) {
                        selected += event.cardId
                    }

                    gameState.copy(selectedCards = selected)
                }

                GameEvents.EndTurn                -> sendC2SEvent(C2SEvent.EndTurnC2SEvent)

                is GameEvents.Wolf                -> client.sendEventToServer(C2SEvent.WolfC2SEvent(event.sheep, event.cardId, event.owner, state.value.player!!.info.id))
                is GameEvents.Wheat               -> client.sendEventToServer(C2SEvent.WheatC2SEvent(event.sheep, event.cardId, event.owner, state.value.player!!.info.id))
                is GameEvents.Yoink               -> client.sendEventToServer(C2SEvent.RequestCardSelectionC2SEvent(event.opponent, event.cardId, state.value.player!!.info.id))
                is GameEvents.SubmitSelectedCards -> client.sendEventToServer(
                    C2SEvent.SelectedCardsC2SEvent(
                        event.cards,
                        event.cardId,
                        event.opponentId,
                        state.value.player!!.info.id
                    )
                )
                is GameEvents.SubmitSelectedSheep -> client.sendEventToServer(C2SEvent.SelectedSheepC2SEvent(event.sheep, state.value.player!!.info.id))
                is GameEvents.SubmitSelectedCardsForSheep -> client.sendEventToServer(C2SEvent.SelectedCardsForSheepC2SEvent(event.cards, state.value.player!!.info.id))

                is GameEvents.RequestCoinFlip -> client.sendEventToServer(C2SEvent.RequestCoinFlipC2SEvent(event.card, event.isHead, event.opponentId, state.value.player!!.info.id))
                GameEvents.FlipCoin -> client.sendEventToServer(C2SEvent.InitiateCoinFlipC2SEvent(state.value.player!!.info.id))
                is GameEvents.ReFlip -> client.sendEventToServer(C2SEvent.ReFlipCoinC2SEvent(event.cardId, state.value.player!!.info.id))
                GameEvents.SkipReFlip -> client.sendEventToServer(C2SEvent.SkipReFlipCoinC2SEvent(state.value.player!!.info.id))
                GameEvents.EndCoinFlip -> client.sendEventToServer(C2SEvent.EndCoinFlipC2SEvent(state.value.player!!.info.id))

                is GameEvents.FixSheep            -> client.sendEventToServer(
                    C2SEvent.FixSheepC2SEvent(
                        event.fixType,
                        event.sheep,
                        event.cardId,
                        event.owner,
                        state.value.player!!.info.id
                    )
                )

                is GameEvents.Discard             -> client.sendEventToServer(C2SEvent.DiscardC2SEvent(event.card, state.value.player!!.info.id))
            }
        }
    }

    private fun clearPendingInput() {
        pendingInput.update { PendingInput() }
    }

    fun fetchRoomsList() {
        viewModelScope.launch {
            val rooms = client.getRoomsList()
            state.update { it.copy(roomsToJoin = rooms) }
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
        }
    }

    fun sendC2SEvent(response: C2SEvent) {
        viewModelScope.launch {
            client.sendEventToServer(response)
            clearPendingInput()
        }
    }

    fun startGame() {
        viewModelScope.launch {
            client.sendEventToServer(C2SEvent.StartGameC2SEvent)
        }
    }
}

@Serializable
data class GameState(
    val player: Player? = null,
    val selectedCards: List<Int> = emptyList(),
    val opponents: Set<Opponent> = emptySet(),
    val clientRoom: ClientRoom? = null,
    val roomsToJoin: List<ClientRoom> = emptyList(),
    val currentTurnPlayer: Long = -1,
    val dialog: GameDialogs = GameDialogs.None,
    val coinFlip: CoinFlip? = null,
)

data class PendingInput(val event: S2CEvent? = null)

sealed interface GameEvents {
    data class ChangeDialog(val dialog: GameDialogs) : GameEvents
    data class ToggleCard(val cardId: Int) : GameEvents
    data object PlayCards : GameEvents
    data class Discard(val card: Card) : GameEvents
    data object EndTurn : GameEvents

    data class Wolf(val sheep: Sheep, val owner: Long, val cardId: Int) : GameEvents
    data class Wheat(val sheep: Sheep, val owner: Long, val cardId: Int) : GameEvents
    data class Yoink(val opponent: Long, val cardId: Int) : GameEvents
    data class RequestCoinFlip(val card: Card, val opponentId: Long, val isHead: Boolean) : GameEvents
    data class SubmitSelectedCards(val cards: List<Int>, val cardId: Int, val opponentId: Long) : GameEvents
    data class SubmitSelectedSheep(val sheep: List<Pair<Sheep, SheepSide?>>) : GameEvents
    data class SubmitSelectedCardsForSheep(val cards: List<Int>) : GameEvents

    data class FixSheep(val fixType: FixSheepType, val sheep: Sheep, val cardId: Int, val owner: Long) : GameEvents

    data object FlipCoin : GameEvents
    data class ReFlip(val cardId: Int) : GameEvents
    data object SkipReFlip : GameEvents
    data object EndCoinFlip : GameEvents
}

sealed interface GameDialogs {
    data object None : GameDialogs
    data class ExceedsMaxHandSize(val extraCards: Int) : GameDialogs
    data class DiscardConfirmation(val card: Card) : GameDialogs
    data class Info(val message: String) : GameDialogs
    data class SelectCards(val amount: Int, val cards: List<Int>, val cardId: Int, val opponentId: Long) : GameDialogs
    data class SelectSheep(val amount: Int, val sheep: List<Sheep>, val selectHalf: Boolean) : GameDialogs
    data class SelectCardsForSheep(val cards: List<Card>) : GameDialogs
    data class SelectCoinFace(val cardId: Int, val opponent: PlayerInfo): GameDialogs
    data object CoinFlip : GameDialogs
}