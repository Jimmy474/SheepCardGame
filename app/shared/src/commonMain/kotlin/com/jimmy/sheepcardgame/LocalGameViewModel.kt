
package com.jimmy.sheepcardgame

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jimmy.sheepcardgame.data.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds

@KoinViewModel
class LocalGameViewModel : ViewModel() {

    val curtain: StateFlow<Curtain>
        field = MutableStateFlow(Curtain())

    val changeScreenRequest = Channel<Pair<Long, Boolean>>(Channel.UNLIMITED)

    val states = mutableStateMapOf<Long, GameState>()
    val dialogs = mutableStateMapOf<Long, MutableList<GameDialogs>>()
    var activePlayerId by mutableLongStateOf(-1L)
        private set

    private val connections = mutableMapOf<Long, LocalConnection>()
    private var initialized = false
    private lateinit var room: Room

    val activeState: GameState?
        get() = states[activePlayerId]

    init{
        viewModelScope.launch {
            for ((playerId, animate) in changeScreenRequest) {
                if (activePlayerId == playerId) continue
                if(!animate){
                    activePlayerId = playerId
                    continue
                }
                triggerCurtain(playerId,false){
                    activePlayerId = playerId
                }
            }
        }
    }

    fun onEvent(event: GameScreenEvents) {
        val playerId = activePlayerId
        when (event) {
            is GameScreenEvents.OpenDialog -> addDialog(playerId, event.dialog)
            GameScreenEvents.CloseDialog -> {
                dialogs[playerId]?.removeFirstOrNull()?.let{ final ->
                    if(final is GameDialogs.FinalRound || final is GameDialogs.GameOver){
                        dialogs.entries.forEach {
                            dialogs[it.key]?.remove(final)
                        }
                    }
                }
            }
            GameScreenEvents.SortHand -> update(playerId) { it.copy(sortedHand = !it.sortedHand) }
            is GameScreenEvents.ToggleCard -> update(playerId) {
                if (!it.selectedCards.remove(event.cardId)) it.selectedCards.add(event.cardId)
                it
            }
            is GameScreenEvents.PlayCards -> playCards(playerId, event.cards)
            GameScreenEvents.EndTurn -> send(C2SEvent.EndTurnC2SEvent(playerId))
            is GameScreenEvents.Wolf -> send(C2SEvent.WolfC2SEvent(event.sheep, event.cardId, event.owner, playerId))
            is GameScreenEvents.Wheat -> send(C2SEvent.WheatC2SEvent(event.sheep, event.cardId, event.owner, playerId))
            is GameScreenEvents.Yoink -> send(C2SEvent.RequestCardSelectionC2SEvent(event.opponent, event.cardId, playerId))
            is GameScreenEvents.SubmitSelectedCards -> send(C2SEvent.SelectedCardsC2SEvent(event.cards, event.cardId, event.opponentId, playerId))
            is GameScreenEvents.SubmitSelectedSheep -> send(C2SEvent.SelectedSheepC2SEvent(event.sheep, playerId))
            is GameScreenEvents.SubmitSelectedCardsForSheep -> send(C2SEvent.SelectedCardsForSheepC2SEvent(event.cards, playerId))
            is GameScreenEvents.RequestCoinFlip -> send(C2SEvent.RequestCoinFlipC2SEvent(event.cardId, event.opponentId, playerId))
            is GameScreenEvents.SelectCoinFace -> send(C2SEvent.SelectFaceCoinFlipC2SEvent(event.isHead, playerId))
            GameScreenEvents.FlipCoin -> send(C2SEvent.FlipCoinC2SEvent(playerId))
            is GameScreenEvents.ReFlip -> send(C2SEvent.ReFlipCoinC2SEvent(event.cardId, playerId))
            GameScreenEvents.SkipReFlip -> send(C2SEvent.SkipReFlipCoinC2SEvent(false, playerId))
            GameScreenEvents.CloseFlip -> send(C2SEvent.SkipReFlipCoinC2SEvent(false, playerId))
            GameScreenEvents.EndCoinFlip -> send(C2SEvent.EndCoinFlipC2SEvent(playerId))
            is GameScreenEvents.FixSheep -> send(C2SEvent.FixSheepC2SEvent(event.fixType, event.sheep, event.cardId, event.owner, playerId))
            is GameScreenEvents.Discard -> send(C2SEvent.DiscardC2SEvent(event.cards, playerId))
            GameScreenEvents.ResetGameScreen -> {
                dialogs.values.forEach { it.clear() }
                changeActivePlayer(room.host.id)
            }
            GameScreenEvents.Leave -> leave()
            is GameScreenEvents.SaveRoomSettings -> send(C2SEvent.RequestRoomSettingsUpdateC2SEvent(event.settings, playerId))
        }
    }

    fun initialize(players: List<String>) {
        require(players.size in 2..4) { "A local game requires between 2 and 4 players" }
        if (initialized) return
        initialized = true

        val host = createConnection(0L, players.first())
        room = Room("LOCAL", host) { }
        connections[host.id] = host
        players.drop(1).forEachIndexed { index, name ->
            val connection = createConnection(index + 1L, name)
            connections[connection.id] = connection
            room.join(connection)
        }
        changeActivePlayer(room.host.id,false)
    }

    fun startGame() {
        if (initialized) connections[room.host.id]?.let {
            viewModelScope.launch {
                triggerCurtain(room.host.id,true){
                    room.handleC2SEvent(it, C2SEvent.StartGameC2SEvent)
                }
            }
        }
    }

    private fun createConnection(id: Long, name: String) = LocalConnection(id, name) { playerId, event ->
        processEvent(playerId, event)
    }

    private fun processEvent(playerId: Long, event: S2CEvent) {
        val state = states[playerId] ?: GameState()
        when (event) {
            is S2CEvent.InitializePlayerS2CEvent -> states[playerId] = state.copy(player = event.player)
            is S2CEvent.InitializeOpponentsS2CEvent -> states[playerId] = state.copy(opponents = mutableStateSetOf<Opponent>().also { it.addAll(event.opponents) })
            is S2CEvent.OpponentJoinedS2CEvent -> state.opponents.add(event.opponent)
            is S2CEvent.OpponentLeftS2CEvent -> state.opponents.remove(event.opponent)
            is S2CEvent.UpdateRoomSettingsS2CEvent -> states[playerId] = state.copy(settings = event.settings)
            is S2CEvent.SyncScoresS2CEvent -> {
                if(playerId != state.clientRoom?.host?.id) return
                state.previousGameScores.clear()
                state.previousGameScores.addAll(event.scores)
            }
            is S2CEvent.UpdatePlayersS2CEvent -> {
                states[playerId] = state.copy(
                    player = event.player,
                    opponents = mutableStateSetOf<Opponent>().also { it.addAll(event.opponents) },
                    currentTurnPlayer = event.activeTurnPlayer,
                    clientRoom = room.asClientRoom(),
                    localAnchorTime = if (room.isStarted) room.startTime else state.localAnchorTime,
                )
                if (room.isStarted) {
                    changeActivePlayer(event.activeTurnPlayer)
                }
            }
            is S2CEvent.UpdateClientRoomS2CEvent -> states[playerId] = state.copy(clientRoom = event.clientRoom)
            is S2CEvent.NotifyGameEventS2CEvent -> {
                state.events.add(0, event.event)
                if (event.event is GameEvents.GoldCardResult) {
                    changeActivePlayer(state.currentTurnPlayer)
                }
            }
            is S2CEvent.SelectFromGivenCardsS2CEvent -> addDialog(playerId, GameDialogs.SelectCards(event.amount, event.cards, event.cardId, event.opponentId))
            is S2CEvent.SelectFromGivenSheepS2CEvent -> addDialog(playerId, GameDialogs.SelectSheep(event.amount, event.sheep, event.selectHalf))
            is S2CEvent.SelectSheepFromGivenCardsS2CEvent -> addDialog(playerId, GameDialogs.SelectCardsForSheep(event.cards))
            is S2CEvent.ExceedsMaxHandSizeS2CEvent -> info(playerId, "You have ${event.extraCards} extra cards in your hand. Discard or use them to continue.")
            is S2CEvent.CoinFlipInitiateS2CEvent -> {
                states[playerId] = state.copy(coinFlip = event.coinFlip)
                addDialog(playerId, GameDialogs.CoinFlip)
                changeActivePlayer(coinFlipPlayer(event.coinFlip))
            }
            is S2CEvent.UpdateCoinFlipS2CEvent -> {
                states[playerId] = state.copy(coinFlip = event.coinFlip)
                changeActivePlayer(coinFlipPlayer(event.coinFlip))
            }
            S2CEvent.CloseCoinFlipS2CEvent -> {
                states[playerId] = state.copy(coinFlip = null)
                dialogs[playerId]?.removeAll { it is GameDialogs.CoinFlip }
            }
            is S2CEvent.NotificationS2CEvent -> info(playerId, event.message)
            S2CEvent.LastTurnS2CEvent -> info(playerId, "This is your last turn, make sure you play all your cards.")
            S2CEvent.FinalRoundS2CEvent -> addDialog(playerId, GameDialogs.FinalRound)
            is S2CEvent.GameOverS2CEvent -> {
                if(playerId == room.host.id) {
                    state.previousGameScores.add(event.points)
                    addDialog(playerId, GameDialogs.GameOver(event.points))
                    changeActivePlayer(room.host.id)
                }
            }
        }
    }

    private fun playCards(playerId: Long, selected: List<Int>) {
        val state = states[playerId] ?: return
        val cards = selected.takeIf { it.isNotEmpty() } ?: state.selectedCards.toList()
        val player = state.player ?: return
        if (cards.isEmpty()) return
        if (cards.size == 1) {
            when (val card = player.hand.firstOrNull { it.id == cards.first() } ?: return) {
                is Card.SheepCard -> info(playerId, "Selected Cards do not make a valid sheep.")
                is Card.ModifierCard -> info(playerId, "Modifier Cards can only be played with Sheep Cards.")
                is Card.SpecialCard -> when (card.specialType) {
                    SpecialType.ReFlip -> info(playerId, "Re Flip can not be played outside of a coin flip.")
                    SpecialType.Wheat, SpecialType.Wolf -> addDialog(playerId, GameDialogs.SelectOpponentSheep(card))
                    SpecialType.Yoink -> addDialog(playerId, GameDialogs.SelectOpponent(card))
                }
                is Card.GoldCard -> addDialog(playerId, GameDialogs.SelectOpponent(card))
            }
        } else if (cards.size > 3) info(playerId, "You must select at most 3 cards to play")
        else if (!GameLogic.isValidSheep(player.hand.filter { it.id in cards })) info(playerId, "Selected Cards do not make a valid sheep")
        else send(C2SEvent.PlayCardsC2SEvent(cards, playerId))
        states[playerId]?.selectedCards?.clear()
    }

    private fun send(event: C2SEvent) {
        val user = when (event) {
            is C2SEvent.LeaveMidGameC2SEvent -> event.user
            is C2SEvent.RequestRoomSettingsUpdateC2SEvent -> event.host
            is C2SEvent.SelectedCardsC2SEvent -> event.user
            is C2SEvent.EndTurnC2SEvent -> event.user
            is C2SEvent.PlayCardsC2SEvent -> event.user
            is C2SEvent.WolfC2SEvent -> event.user
            is C2SEvent.WheatC2SEvent -> event.user
            is C2SEvent.RequestCardSelectionC2SEvent -> event.user
            is C2SEvent.RequestCoinFlipC2SEvent -> event.user
            is C2SEvent.SelectFaceCoinFlipC2SEvent -> event.user
            is C2SEvent.FlipCoinC2SEvent -> event.user
            is C2SEvent.ReFlipCoinC2SEvent -> event.user
            is C2SEvent.SkipReFlipCoinC2SEvent -> event.user
            is C2SEvent.EndCoinFlipC2SEvent -> event.user
            is C2SEvent.SelectedSheepC2SEvent -> event.user
            is C2SEvent.SelectedCardsForSheepC2SEvent -> event.user
            is C2SEvent.FixSheepC2SEvent -> event.user
            is C2SEvent.DiscardC2SEvent -> event.user
            C2SEvent.StartGameC2SEvent -> room.host.id
        }
        connections[user]?.let { room.handleC2SEvent(it, event) }
    }

    private fun update(playerId: Long, transform: (GameState) -> GameState) {
        states[playerId]?.let { states[playerId] = transform(it) }
    }

    private fun changeActivePlayer(playerId: Long, animate: Boolean = true) {
        changeScreenRequest.trySend(playerId to animate)
    }

    suspend fun triggerCurtain(id: Long? = null, skipEntry: Boolean, halfway: () -> Unit) {
        curtain.update{
            Curtain(visible = true, name = activeState?.let{ it.getName(id ?: it.currentTurnPlayer) } ?: "Host", skipEntry = skipEntry)
        }
        delay((if(skipEntry) 200 else 400).milliseconds)
        halfway()
        delay(200.milliseconds)
        curtain.update{ it.copy(visible = false) }
        delay(400.milliseconds)
    }

    private fun addDialog(playerId: Long, dialog: GameDialogs) {
        if (playerId == -1L) return
        val playerDialogs = dialogs.getOrPut(playerId) { mutableStateListOf() }
        if (dialog !in playerDialogs) playerDialogs.add(dialog)
    }

    private fun info(playerId: Long, message: String) = addDialog(playerId, GameDialogs.Info(message))

    private fun coinFlipPlayer(flip: CoinFlip): Long {
        if (flip.currentResult == null) {
            return if (flip.playerChoice == null) flip.attacker else flip.target
        }

        val winner = flip.winner
        val eligible = connections.keys
            .filter { id ->
                id !in flip.skippedReFlip &&
                    id !in flip.closedDialog &&
                    states[id]?.player?.hand?.any { card ->
                        card is Card.SpecialCard && card.specialType == SpecialType.ReFlip
                    } == true
            }
            .filter { it != winner }

        return eligible.firstOrNull() ?: winner
    }

    private fun leave() {
        if (initialized) connections[activePlayerId]?.let { room.leave(it.id) }
        states.clear()
        dialogs.clear()
        connections.clear()
        initialized = false
        changeActivePlayer(-1L)
    }

    data class Curtain(val visible: Boolean = false, val name: String = "", val skipEntry: Boolean = false)
}
