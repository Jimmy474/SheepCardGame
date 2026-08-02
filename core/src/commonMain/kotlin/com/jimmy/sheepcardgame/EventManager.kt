package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.data.*
import kotlinx.serialization.Serializable

@Serializable
sealed interface S2CEvent {

    @Serializable
    data class InitializePlayerS2CEvent(val player: Player) : S2CEvent

    @Serializable
    data class UpdateRoomSettingsS2CEvent(val settings: RoomSettings) : S2CEvent
    @Serializable
    data class InitializeOpponentsS2CEvent(val opponents: Set<Opponent>) : S2CEvent

    @Serializable
    data class UpdatePlayersS2CEvent(val player: Player, val opponents: Set<Opponent>, val activeTurnPlayer: Long) : S2CEvent

    @Serializable
    data class SelectFromGivenCardsS2CEvent(val amount: Int, val cards: List<Int>, val cardId: Int, val opponentId: Long) : S2CEvent

    @Serializable
    data class OpponentJoinedS2CEvent(val opponent: Opponent) : S2CEvent

    @Serializable
    data class OpponentLeftS2CEvent(val opponent: Opponent) : S2CEvent

    @Serializable
    data class UpdateClientRoomS2CEvent(val clientRoom: ClientRoom) : S2CEvent

    @Serializable
    data class ExceedsMaxHandSizeS2CEvent(val extraCards: Int) : S2CEvent

    @Serializable
    data class CoinFlipInitiateS2CEvent(val coinFlip: CoinFlip) : S2CEvent

    @Serializable
    data class UpdateCoinFlipS2CEvent(val coinFlip: CoinFlip) : S2CEvent

    @Serializable
    data object CloseCoinFlipS2CEvent : S2CEvent

    @Serializable
    data class SelectFromGivenSheepS2CEvent(val amount: Int, val sheep: List<Sheep>, val selectHalf: Boolean) : S2CEvent

    @Serializable
    data class SelectSheepFromGivenCardsS2CEvent(val cards: List<Card>) : S2CEvent

    @Serializable
    data class NotificationS2CEvent(val message: String) : S2CEvent

    @Serializable
    data object LastTurnS2CEvent : S2CEvent

    @Serializable
    data object FinalRoundS2CEvent : S2CEvent

    @Serializable
    data class SyncScoresS2CEvent(val scores: List<List<Pair<String,Int>>>) : S2CEvent

    @Serializable
    data class GameOverS2CEvent(val points: List<Pair<String,Int>>) : S2CEvent

    @Serializable
    data class NotifyGameEventS2CEvent(val event: GameEvents) : S2CEvent
}

@Serializable
sealed interface C2SEvent {

    @Serializable
    data class LeaveMidGameC2SEvent(val user: Long) : C2SEvent

    @Serializable
    data class RequestCardSelectionC2SEvent(val opponent: Long, val cardId: Int, val user: Long) : C2SEvent

    @Serializable
    data class RequestCoinFlipC2SEvent(val cardId: Int, val opponent: Long, val user: Long) : C2SEvent

    @Serializable
    data class SelectFaceCoinFlipC2SEvent(val isHead: Boolean, val user: Long) : C2SEvent

    @Serializable
    data class FlipCoinC2SEvent(val user: Long) : C2SEvent

    @Serializable
    data class ReFlipCoinC2SEvent(val cardId: Int, val user: Long) : C2SEvent

    @Serializable
    data class SkipReFlipCoinC2SEvent(val permanent: Boolean, val user: Long) : C2SEvent

    @Serializable
    data class EndCoinFlipC2SEvent(val user: Long) : C2SEvent

    @Serializable
    data class SelectedCardsC2SEvent(val cards: List<Int>, val cardId: Int, val opponentId: Long, val user: Long) : C2SEvent

    @Serializable
    data class SelectedSheepC2SEvent(val sheep: List<Pair<Sheep, SheepSide?>>, val user: Long) : C2SEvent

    @Serializable
    data class SelectedCardsForSheepC2SEvent(val cards: List<Int>, val user: Long) : C2SEvent

    @Serializable
    data object StartGameC2SEvent : C2SEvent

    @Serializable
    data class EndTurnC2SEvent(val user: Long) : C2SEvent

    @Serializable
    data class PlayCardsC2SEvent(val cards: List<Int>, val user: Long) : C2SEvent


    @Serializable
    data class WolfC2SEvent(val sheep: Sheep, val cardId: Int, val owner: Long, val user: Long) : C2SEvent

    @Serializable
    data class WheatC2SEvent(val sheep: Sheep, val cardId: Int, val owner: Long, val user: Long) : C2SEvent

    @Serializable
    data class FixSheepC2SEvent(val fixType: FixSheepType, val sheep: Sheep, val cardId: Int, val owner: Long, val user: Long) : C2SEvent

    @Serializable
    data class DiscardC2SEvent(val cards: List<Card>, val user: Long) : C2SEvent

    @Serializable
    data class RequestRoomSettingsUpdateC2SEvent(val settings: RoomSettings, val host: Long) : C2SEvent
}
