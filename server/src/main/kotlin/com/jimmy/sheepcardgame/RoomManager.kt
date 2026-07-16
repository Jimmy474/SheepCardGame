package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.data.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object RoomManager {
    val rooms = ConcurrentHashMap<String, Room>()

    fun newRoom(host: Connection): Room {
        val code = generateRoomCode()
        rooms[code] = Room(code, host)
        return rooms[code]!!
    }

    private fun generateRoomCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        var code: String
        do {
            code = (0..5).map { allowedChars.random() }.joinToString("")
        } while (rooms.containsKey(code))
        return code
    }
}

data class PendingGoldCardAction(
    val card: Card.GoldCard,
    val winner: Long,
    val looser: Long
)

class Room(val code: String, var host: Connection) {

    companion object {
        const val MAX_PLAYERS = 4
        const val INITIAL_HAND_SIZE = 5
        const val MIN_HAND_SIZE = 3
        const val MAX_HAND_SIZE = 7
    }

    private val players = ConcurrentHashMap<Long, Player>()
    private val sockets = ConcurrentHashMap<Long, Connection>()

    private var deck = Deck()

    var isStarted = false
    val queue = ArrayDeque<Long>()
    var currentTurnPlayer: Long = -1
    var coinFlip: CoinFlip? = null
    var pendingGoldCardAction: PendingGoldCardAction? = null

    init {
        join(host)
    }

    fun handleC2SEvent(connection: Connection, event: C2SEvent) {
        when (event) {
            is C2SEvent.SelectedOpponentC2SEvent      -> {}

            is C2SEvent.SelectCoinFaceC2SEvent        -> {}

            is C2SEvent.SelectedCardsC2SEvent         -> transferCards(event.cards, event.cardId, event.opponentId, event.user)

            C2SEvent.StartGameC2SEvent                -> tryStartGame(connection)

            C2SEvent.EndTurnC2SEvent                  -> nextTurn()
            is C2SEvent.PlayCardsC2SEvent             -> playCards(event.cards, event.user)
            is C2SEvent.DiscardC2SEvent               -> discardCard(event.card, event.user)

            is C2SEvent.WolfC2SEvent                  -> useWolf(event.sheep, event.cardId, event.owner, event.user)
            is C2SEvent.WheatC2SEvent                 -> useWheat(event.sheep, event.cardId, event.owner, event.user)
            is C2SEvent.RequestCardSelectionC2SEvent  -> approveCardSelectionRequest(event.opponent, event.cardId, event.user)
            is C2SEvent.RequestCoinFlipC2SEvent       -> approveCoinFlip(event.card, event.isHead, event.opponent, event.user)
            is C2SEvent.InitiateCoinFlipC2SEvent      -> initiateCoinFlip(event.user)
            is C2SEvent.ReFlipCoinC2SEvent            -> reFlipCoin(event.cardId, event.user)
            is C2SEvent.SkipReFlipCoinC2SEvent        -> skipReFlipCoin(event.user)
            is C2SEvent.EndCoinFlipC2SEvent           -> endCoinFlip(event.user)

            is C2SEvent.SelectedSheepC2SEvent         -> completeGoldCardPendingAction(event.sheep, event.user)
            is C2SEvent.SelectedCardsForSheepC2SEvent -> validateSheepFromCard(event.cards, event.user)

            is C2SEvent.FixSheepC2SEvent              -> {
                when (event.fixType) {
                    FixSheepType.Franken -> deFrankenSheep(event.sheep, event.cardId, event.owner, event.user)
                    FixSheepType.Paint   -> dePaintSheep(event.sheep, event.cardId, event.owner, event.user)
                    FixSheepType.Rainbow -> deRainbowSheep(event.sheep, event.cardId, event.owner, event.user)
                }
            }
        }
    }

    fun isFull() = sockets.size >= MAX_PLAYERS

    fun updateClientRoom() = broadcast(S2CEvent.UpdateClientRoomS2CEvent(asClientRoom()).encodeToString())

    fun join(connection: Connection): Boolean {
        if (sockets.size >= MAX_PLAYERS || isStarted) return false

        val player = Player(PlayerInfo(connection.id, connection.name))
        sockets[connection.id] = connection
        players[connection.id] = player
        sendIndividualMessage(connection.id, S2CEvent.InitializePlayerS2CEvent(player).encodeToString())

        broadcast(S2CEvent.OpponentJoinedS2C(player.asOpponent()).encodeToString(), exclude = connection)
        updateClientRoom()

        return true
    }

    fun leave(connection: Connection): Boolean {
        val opponent = players[connection.id]?.asOpponent()
        sockets.remove(connection.id)
        players.remove(connection.id)
        broadcast(S2CEvent.OpponentLeftS2C(opponent!!).encodeToString(), exclude = connection)

        return sockets.isEmpty()
    }

    fun changeHost() {
        host = sockets.values.random()
    }

    fun updatePlayers() {
        val scope = CoroutineScope(Dispatchers.Default)
        sockets.forEach { (id, connection) ->
            val player = players[id] ?: return@forEach
            scope.launch {
                val opponents = players
                    .filterNot { it.key == connection.id }
                    .map { it.value.asOpponent() }
                    .toSet()

                connection.session.send(S2CEvent.UpdatePlayersS2CEvent(player, opponents, currentTurnPlayer).encodeToString())
            }
        }
        updateClientRoom()
    }

    fun tryStartGame(connection: Connection) {
        if (players[connection.id]?.info?.id != host.id) return
        isStarted = true
        players.forEach { (id, player) ->
            players[id] = player.copy(hand = getCardsFromDeck(INITIAL_HAND_SIZE))
            queue.add(id)
        }
        nextTurn()
    }

    fun nextTurn() {
        val previousCon = queue.last()
        val previousPlayer = players[previousCon]!!
        if (previousPlayer.hand.size > MAX_HAND_SIZE) {
            sendIndividualMessage(previousCon, S2CEvent.ExceedsMaxHandSizeS2CEvent(previousPlayer.hand.size - MAX_HAND_SIZE).encodeToString())
            return
        }

        val nextId = queue.removeFirst()
        currentTurnPlayer = nextId
        val nextPlayer = players[nextId]!!
        val drawnCards = getCardsFromDeck(maxOf(MIN_HAND_SIZE - nextPlayer.hand.size, 1))
        players[nextId] = nextPlayer.copy(hand = nextPlayer.hand + drawnCards)
        if (drawnCards.isNotEmpty()) queue.add(nextId)
        updatePlayers()
    }

    fun getCardsFromDeck(amount: Int): List<Card> {
        val randomCards = deck.cards.shuffled().take(amount)
        deck.cards.removeAll(randomCards)
        return randomCards
    }

    fun turnCheck(user: Long): Boolean = queue.lastOrNull { it == user } != null

    fun useWolf(sheep: Sheep, cardId: Int, owner: Long, user: Long) {
        if (!turnCheck(user)) return

        val card = players[user]?.hand?.filterIsInstance<Card.SpecialCard>()?.firstOrNull { it.specialType == SpecialType.Wolf && it.id == cardId } ?: return
        manageSheep(sheep, owner = owner, add = false)
        manageCards(card, owner = user, add = false)
        deck.discardPile += sheep.cards + card
        updatePlayers()
    }

    fun useWheat(sheep: Sheep, cardId: Int, owner: Long, user: Long) {
        if (!turnCheck(user)) return

        val card = players[user]?.hand?.filterIsInstance<Card.SpecialCard>()?.firstOrNull { it.specialType == SpecialType.Wheat && it.id == cardId } ?: return
        manageSheep(sheep, owner = owner, add = false)
        manageSheep(sheep, owner = user)
        manageCards(card, owner = user, add = false)
        deck.discardPile += card
        updatePlayers()
    }

    fun approveCardSelectionRequest(opponentId: Long, cardId: Int, user: Long) {
        if (!turnCheck(user)) return
        val card = players[user]?.hand?.filterIsInstance<Card.SpecialCard>()?.firstOrNull { it.id == cardId && it.specialType == SpecialType.Yoink } ?: return
        val opponent = players[opponentId] ?: return
        val opponentHand = opponent.hand.map { it.id }
        sendIndividualMessage(user, S2CEvent.SelectFromGivenCardsS2CEvent(minOf(2, opponentHand.size), opponentHand, card.id, opponentId).encodeToString())
    }

    fun transferCards(cardIds: List<Int>, cardId: Int, opponentId: Long, user: Long) {
        if (!turnCheck(user)) return
        val cards = players[opponentId]?.hand?.filter { it.id in cardIds } ?: return
        val card = players[user]?.hand?.filterIsInstance<Card.SpecialCard>()?.firstOrNull { it.id == cardId && it.specialType == SpecialType.Yoink } ?: return
        manageCards(*cards.toTypedArray(), owner = opponentId, add = false)
        manageCards(*cards.toTypedArray(), owner = user)
        manageCards(card, owner = user, add = false)
        deck.discardPile += card
        updatePlayers()
    }

    fun approveCoinFlip(card: Card, isHead: Boolean, opponentId: Long, user: Long) {
        if (!turnCheck(user)) return
        val card = players[user]?.hand?.filterIsInstance<Card.GoldCard>()?.firstOrNull { it.id == card.id } ?: return
        manageCards(card, owner = user, add = false)
        deck.discardPile += card
        CoinFlip(
            goldCard = card,
            attacker = user,
            target = opponentId,
            playerChoice = isHead,
            currentResult = null,
            canReFlip = queue.sumOf { id ->
                players[id]?.hand?.count { it is Card.SpecialCard && it.specialType == SpecialType.ReFlip } ?: 0
            } > 0,
            skippedReFlip = emptyList()
        ).let {
            coinFlip = it
            updatePlayers()
            broadcast(S2CEvent.CoinFlipInitiateS2CEvent(it).encodeToString())
        }
    }

    fun initiateCoinFlip(user: Long) {
        coinFlip?.let { flip ->
            if (flip.target != user) return

            flip.copy(currentResult = Random.nextBoolean(), iteration = flip.iteration + 1).let {
                coinFlip = it
                broadcast(S2CEvent.CoinFlipInitiateS2CEvent(it).encodeToString())
            }
        }
    }

    fun reFlipCoin(cardId: Int, user: Long) {
        coinFlip?.let { flip ->
            val card = players[user]?.hand?.firstOrNull { it.id == cardId } ?: return
            manageCards(card, owner = user, add = false)
            deck.discardPile += card
            val reFlipAmount = queue.sumOf { id ->
                players[id]?.hand?.count { it is Card.SpecialCard && it.specialType == SpecialType.ReFlip } ?: 0
            }
            flip.copy(canReFlip = reFlipAmount > 0, currentResult = Random.nextBoolean(), skippedReFlip = emptyList(), iteration = flip.iteration + 1).let {
                updatePlayers()
                broadcast(S2CEvent.CoinFlipInitiateS2CEvent(it).encodeToString())
            }
        }

    }

    fun skipReFlipCoin(user: Long) {
        coinFlip?.let { flip ->
            val hasReFlipCard = players[user]?.hand?.any { it is Card.SpecialCard && it.specialType == SpecialType.ReFlip } ?: false
            if (!hasReFlipCard) return

            val reFlipAmount = queue.sumOf { id ->
                players[id]?.hand?.count { it is Card.SpecialCard && it.specialType == SpecialType.ReFlip } ?: 0
            }
            val skippedReFlip = flip.skippedReFlip + user
            flip.copy(skippedReFlip = skippedReFlip, canReFlip = reFlipAmount > skippedReFlip.size).let {
                coinFlip = it
                broadcast(S2CEvent.CoinFlipInitiateS2CEvent(it).encodeToString())
            }
        }
    }

    fun endCoinFlip(user: Long) {
        coinFlip?.let {
            if (it.currentResult == null) return
            val winner = if (it.currentResult == it.playerChoice) it.attacker else it.target
            val looser = if (it.currentResult != it.playerChoice) it.attacker else it.target
            if (user != winner) return

            when (it.goldCard.goldCardType) {
                GoldCardType.Lure    -> flockSheepSelection(it.goldCard, looser, winner, 2)
                GoldCardType.Yoink   -> yoinkEntireHand(looser, winner)
                GoldCardType.Halve   -> flockSheepSelection(it.goldCard, looser, winner, 2, true)
                GoldCardType.Remove  -> flockSheepSelection(it.goldCard, looser, winner, 2)
                GoldCardType.Recover -> discardPileSheepSelection(it.goldCard, looser, winner)
            }
        }
    }

    private fun yoinkEntireHand(looser: Long, winner: Long) {
        val hand = players[looser]?.hand ?: return
        manageCards(*hand.toTypedArray(), owner = looser, add = false)
        manageCards(*hand.toTypedArray(), owner = winner)
        updatePlayers()
        coinFlip = null
        broadcast(S2CEvent.CloseCoinFlipS2CEvent.encodeToString())
    }

    private fun flockSheepSelection(card: Card.GoldCard, looser: Long, winner: Long, amount: Int, selectHalf: Boolean = false) {
        val sheep = players[looser]?.info?.flock?.sheep ?: return
        pendingGoldCardAction = PendingGoldCardAction(card, winner, looser)
        if (sheep.isEmpty()) {
            completeGoldCardPendingAction(emptyList(), winner)
            sendIndividualMessage(winner, S2CEvent.NotificationS2CEvent("No Selectable sheep is present.").encodeToString())
            return
        }
        sendIndividualMessage(winner, S2CEvent.SelectFromGivenSheepS2CEvent(amount, sheep, selectHalf).encodeToString())
    }

    private fun discardPileSheepSelection(card: Card.GoldCard, looser: Long, winner: Long) {
        pendingGoldCardAction = PendingGoldCardAction(card, winner, looser)
        val cards = deck.discardPile.filter { it is Card.SheepCard || it is Card.ModifierCard }
        if (cards.isEmpty() || GameLogic.getSheep(deck.discardPile).isEmpty()) {
            completeGoldCardPendingAction(emptyList(), winner)
            sendIndividualMessage(winner, S2CEvent.NotificationS2CEvent("Discard pile doesnt have cards that can make valid sheep").encodeToString())
            return
        }
        sendIndividualMessage(winner, S2CEvent.SelectSheepFromGivenCardsS2CEvent(cards).encodeToString())
    }

    private fun validateSheepFromCard(cardIds: List<Int>, user: Long) {
        val cards = deck.discardPile.filter { it.id in cardIds }
        if (!GameLogic.isValidSheep(cards)) return
        completeGoldCardPendingAction(listOf(GameLogic.buildSheep(cards)!! to null), user)
    }

    private fun completeGoldCardPendingAction(sheep: List<Pair<Sheep, SheepSide?>>, user: Long) {
        pendingGoldCardAction?.let { action ->
            if (action.winner != user) return

            when (action.card.goldCardType) {
                GoldCardType.Remove  -> {
                    manageSheep(*sheep.map { it.first }.toTypedArray(), owner = action.looser, add = false)
                    sheep.forEach {
                        deck.discardPile += it.first.cards
                    }
                }

                GoldCardType.Lure    -> {
                    manageSheep(*sheep.map { it.first }.toTypedArray(), owner = action.looser, add = false)
                    manageSheep(*sheep.map { it.first }.toTypedArray(), owner = action.winner)
                }

                GoldCardType.Recover -> {
                    deck.discardPile -= sheep.first().first.cards.toSet()
                    manageSheep(*sheep.map { it.first }.toTypedArray(), owner = action.winner)
                }

                GoldCardType.Halve   -> {
                    sheep.forEach { (s, side) ->
                        manageSheep(s, owner = action.looser, add = false)
                        manageCards(if (side == SheepSide.Front) s.head else s.butt, owner = action.winner)
                        manageCards(if (side == SheepSide.Front) s.butt else s.head, owner = action.looser)
                        s.modifier?.let { manageCards(it, owner = action.winner) }
                    }
                }

                GoldCardType.Yoink   -> return@let
            }

            pendingGoldCardAction = null
            coinFlip = null
            updatePlayers()
            broadcast(S2CEvent.CloseCoinFlipS2CEvent.encodeToString())
        }
    }

    fun deFrankenSheep(sheep: Sheep, cardId: Int, owner: Long, user: Long) {
        if (!turnCheck(user)) return
        val card = players[user]?.hand?.filterIsInstance<Card.SheepCard>()?.firstOrNull { it.id == cardId } ?: return
        val deFrankenSide = GameLogic.deFrankenReplacement(sheep, card) ?: return
        repairSheep(deFrankenSide, sheep, card, owner, user)
        updatePlayers()
    }

    fun dePaintSheep(sheep: Sheep, cardId: Int, owner: Long, user: Long) {
        if (!turnCheck(user)) return
        val card = players[user]?.hand?.filterIsInstance<Card.SheepCard>()?.firstOrNull { it.id == cardId } ?: return
        val dePaintSide = GameLogic.dePaintReplacement(sheep, card) ?: return
        repairSheep(dePaintSide, sheep, card, owner, user)
        updatePlayers()
    }

    fun deRainbowSheep(sheep: Sheep, cardId: Int, owner: Long, user: Long) {
        if (!turnCheck(user)) return
        val card = players[user]?.hand?.filterIsInstance<Card.SheepCard>()?.firstOrNull { it.id == cardId } ?: return
        val deRainbowSide = GameLogic.deRainbowReplacement(sheep, card) ?: return
        repairSheep(deRainbowSide, sheep, card, owner, user)
        updatePlayers()
    }

    private fun repairSheep(side: SheepSide, sheep: Sheep, card: Card.SheepCard, owner: Long, user: Long) {
        val newSheep = when (side) {
            SheepSide.Front -> GameLogic.buildSheep(listOf(sheep.butt, card))
            SheepSide.Back  -> GameLogic.buildSheep(listOf(sheep.head, card))
        } ?: return
        manageSheep(sheep, owner = owner, add = false)
        manageSheep(newSheep, owner = owner)

        manageCards(card, owner = user, add = false)
        sheep.modifier?.let { manageCards(it, owner = user) }
        when (side) {
            SheepSide.Front -> manageCards(sheep.head, owner = user)
            SheepSide.Back  -> manageCards(sheep.butt, owner = user)
        }
    }

    fun discardCard(card: Card, user: Long) {
        manageCards(card, owner = user, add = false)
        deck.discardPile += card
        updatePlayers()
    }

    fun playCards(cardIds: List<Int>, user: Long) {
        if (!turnCheck(user)) return

        val cards = players[user]?.hand?.filter { cardIds.contains(it.id) } ?: return
        val sheep = GameLogic.buildSheep(cards) ?: return
        manageSheep(sheep, owner = user)
        manageCards(*cards.toTypedArray(), owner = user, add = false)
        updatePlayers()
        println("cards played: $cards by user $user")
    }

    fun manageSheep(vararg sheep: Sheep, owner: Long, add: Boolean = true) {
        val affectedCon = queue.firstOrNull { it == owner } ?: return
        val affectedPlayer = players[affectedCon] ?: return

        val newSheep = if (add) {
            affectedPlayer.info.flock.sheep + sheep
        } else {
            sheep.fold(affectedPlayer.info.flock.sheep) { currentFlock, singleSheep ->
                currentFlock - singleSheep
            }
        }

        players[affectedCon] = affectedPlayer.copy(
            info = affectedPlayer.info.copy(
                flock = affectedPlayer.info.flock.copy(
                    sheep = newSheep
                )
            )
        )
    }

    fun manageCards(vararg cards: Card, owner: Long, add: Boolean = true) {
        val affectedCon = queue.firstOrNull { it == owner } ?: return
        val affectedPlayer = players[affectedCon] ?: return

        val newHand = if (add) {
            affectedPlayer.hand + cards
        } else {
            cards.fold(affectedPlayer.hand) { currentHand, singleCard ->
                currentHand - singleCard
            }
        }

        players[affectedCon] = affectedPlayer.copy(
            hand = newHand
        )
    }

    fun broadcast(message: String, exclude: Connection? = null) {
        val scope = CoroutineScope(Dispatchers.Default)
        sockets.forEach { (_, connection) ->
            if (connection != exclude) scope.launch { connection.session.send(message) }
        }
    }

    fun sendIndividualMessage(id: Long, message: String) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            sockets[id]?.session?.send(message)
        }
    }

    fun asClientRoom() = ClientRoom(code, sockets.size, players[host.id]!!.info, deck.cards.size, deck.discardPile.size, isStarted)
}
