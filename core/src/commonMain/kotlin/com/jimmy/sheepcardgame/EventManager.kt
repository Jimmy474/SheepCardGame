package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.data.Card
import com.jimmy.sheepcardgame.data.ClientRoom
import com.jimmy.sheepcardgame.data.CoinFlip
import com.jimmy.sheepcardgame.data.FixSheepType
import com.jimmy.sheepcardgame.data.Opponent
import com.jimmy.sheepcardgame.data.Player
import com.jimmy.sheepcardgame.data.Sheep
import com.jimmy.sheepcardgame.data.SheepSide
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface S2CEvent {

    @Serializable
    data class InitializePlayerS2CEvent(val player: Player) : S2CEvent

    @Serializable
    data class InitializeOpponentsS2CEvent(val opponents: Set<Opponent>) : S2CEvent

    @Serializable
    data class UpdatePlayersS2CEvent(val player: Player, val opponents: Set<Opponent>, val activeTurnPlayer: Long) : S2CEvent

    @Serializable
    data class SelectFromGivenCardsS2CEvent(val amount: Int, val cards: List<Int>, val cardId: Int, val opponentId: Long) : S2CEvent

    @Serializable
    data class OpponentJoinedS2C(val opponent: Opponent) : S2CEvent

    @Serializable
    data class OpponentLeftS2C(val opponent: Opponent) : S2CEvent

    @Serializable
    data class UpdateClientRoomS2CEvent(val clientRoom: ClientRoom) : S2CEvent

    @Serializable
    data class ExceedsMaxHandSizeS2CEvent(val extraCards: Int) : S2CEvent

    @Serializable
    data class CoinFlipInitiateS2CEvent(val coinFlip: CoinFlip) : S2CEvent

    @Serializable
    data object CloseCoinFlipS2CEvent : S2CEvent

    @Serializable
    data class SelectFromGivenSheepS2CEvent(val amount: Int, val sheep: List<Sheep>, val selectHalf: Boolean): S2CEvent

    @Serializable
    data class SelectSheepFromGivenCardsS2CEvent(val cards: List<Card>): S2CEvent

    @Serializable
    data class NotificationS2CEvent(val message: String) : S2CEvent

    fun encodeToString(): String = Json.encodeToString(this)

    companion object {
        fun decodeFromString(input: String): S2CEvent = Json.decodeFromString(input)
    }
}

@Serializable
sealed interface C2SEvent {

    @Serializable
    data class RequestCardSelectionC2SEvent(val opponent: Long, val cardId: Int, val user: Long) : C2SEvent

    @Serializable
    data class RequestCoinFlipC2SEvent(val card: Card, val isHead: Boolean, val opponent: Long, val user: Long) : C2SEvent

    @Serializable
    data class InitiateCoinFlipC2SEvent(val user: Long) : C2SEvent

    @Serializable
    data class ReFlipCoinC2SEvent(val cardId: Int, val user: Long) : C2SEvent

    @Serializable
    data class SkipReFlipCoinC2SEvent(val user: Long) : C2SEvent

    @Serializable
    data class EndCoinFlipC2SEvent(val user: Long) : C2SEvent

    @Serializable
    data class SelectedCardsC2SEvent(val cards: List<Int>, val cardId: Int, val opponentId: Long, val user: Long) : C2SEvent

    @Serializable
    data class SelectedSheepC2SEvent(val sheep: List<Pair<Sheep,SheepSide?>>, val user: Long) : C2SEvent

    @Serializable
    data class SelectedCardsForSheepC2SEvent(val cards: List<Int>, val user: Long) : C2SEvent

    @Serializable
    data class SelectedOpponentC2SEvent(val opponent: Opponent) : C2SEvent

    @Serializable
    data class SelectCoinFaceC2SEvent(val isHead: Boolean) : C2SEvent

    @Serializable
    data object StartGameC2SEvent : C2SEvent

    @Serializable
    data object EndTurnC2SEvent : C2SEvent

    @Serializable
    data class PlayCardsC2SEvent(val cards: List<Int>, val user: Long) : C2SEvent


    @Serializable
    data class WolfC2SEvent(val sheep: Sheep, val cardId: Int, val owner: Long, val user: Long) : C2SEvent

    @Serializable
    data class WheatC2SEvent(val sheep: Sheep, val cardId: Int, val owner: Long, val user: Long) : C2SEvent

    @Serializable
    data class FixSheepC2SEvent(val fixType: FixSheepType, val sheep: Sheep, val cardId: Int, val owner: Long, val user: Long) : C2SEvent

    @Serializable
    data class DiscardC2SEvent(val card: Card, val user: Long) : C2SEvent


    fun encodeToString(): String = Json.encodeToString(this)

    companion object {
        fun decodeFromString(input: String): C2SEvent = Json.decodeFromString(input)
    }
}
