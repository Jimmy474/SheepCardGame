package com.jimmy.sheepcardgame

import com.jimmy.sheepcardgame.GameLogic.MAX_PLAYERS
import com.jimmy.sheepcardgame.data.*
import kotlin.random.Random
import kotlin.time.Clock

data class PendingGoldCardAction(
    val card: Card.GoldCard,
    val winner: Long,
    val looser: Long
)

class Room(val code: String, var host: Connection, val onDestroy: (String) -> Unit) {

    internal val players = mutableMapOf<Long, Player>()
    internal val sockets = mutableMapOf<Long, Connection>()

    internal var deck = Deck()
        private set

    internal var roomSettings = RoomSettings()
        private set
    internal val gameEvents = mutableListOf<GameEvents>()
    private val cardsLifeCycle = mutableMapOf<Int, List<Int>>()
    private var eventIds = 0
        get() = field++
    var isStarted = false
    internal val queue = ArrayDeque<Long>()
    internal var usedGoldCardForTurn = false
    internal var currentTurnPlayer: Long = -1
        private set
    internal var coinFlip: CoinFlip? = null
        private set
    internal var pendingGoldCardAction: PendingGoldCardAction? = null
        private set
    internal var finalRoundAnnounced = false
        private set

    var startTime = 0L
        private set
    internal val previousGameScores = mutableListOf<List<Pair<String, Int>>>()

    init {
        join(host)
    }

    fun handleC2SEvent(connection: Connection, event: C2SEvent) {
        when (event) {
            is C2SEvent.LeaveMidGameC2SEvent              -> leave(event.user)
            is C2SEvent.RequestRoomSettingsUpdateC2SEvent -> updateRoomSettings(event.settings, event.host)
            is C2SEvent.SelectedCardsC2SEvent             -> transferCards(event.cards, event.cardId, event.opponentId, event.user)

            C2SEvent.StartGameC2SEvent                    -> tryStartGame(connection)

            is C2SEvent.EndTurnC2SEvent                   -> nextTurn(event.user)
            is C2SEvent.PlayCardsC2SEvent                 -> playCards(event.cards, event.user)
            is C2SEvent.DiscardC2SEvent                   -> discardCard(event.cards, event.user)

            is C2SEvent.WolfC2SEvent                      -> useWolf(event.sheep, event.cardId, event.owner, event.user)
            is C2SEvent.WheatC2SEvent                     -> useWheat(event.sheep, event.cardId, event.owner, event.user)
            is C2SEvent.RequestCardSelectionC2SEvent      -> approveCardSelectionRequest(event.opponent, event.cardId, event.user)

            is C2SEvent.RequestCoinFlipC2SEvent           -> approveCoinFlip(event.cardId, event.opponent, event.user)
            is C2SEvent.SelectFaceCoinFlipC2SEvent        -> selectCoinFace(event.isHead, event.user)
            is C2SEvent.FlipCoinC2SEvent                  -> flipTheCoin(event.user)
            is C2SEvent.ReFlipCoinC2SEvent                -> reFlipCoin(event.cardId, event.user)
            is C2SEvent.SkipReFlipCoinC2SEvent            -> skipReFlipCoin(event.permanent, event.user)
            is C2SEvent.EndCoinFlipC2SEvent               -> endCoinFlip(event.user)

            is C2SEvent.SelectedSheepC2SEvent             -> completeGoldCardPendingAction(event.sheep, event.user)
            is C2SEvent.SelectedCardsForSheepC2SEvent     -> validateSheepFromCard(event.cards, event.user)

            is C2SEvent.FixSheepC2SEvent                  -> {
                when (event.fixType) {
                    FixSheepType.Franken -> deFrankenSheep(event.sheep, event.cardId, event.owner, event.user)
                    FixSheepType.Paint   -> dePaintSheep(event.sheep, event.cardId, event.owner, event.user)
                    FixSheepType.Rainbow -> deRainbowSheep(event.sheep, event.cardId, event.owner, event.user)
                }
            }
        }
    }

    fun isFull() = sockets.size >= MAX_PLAYERS

    fun updateClientRoom() = broadcast(S2CEvent.UpdateClientRoomS2CEvent(asClientRoom()))

    fun join(connection: Connection): Boolean {
        if (sockets.size >= MAX_PLAYERS || isStarted) return false

        val player = Player(PlayerInfo(connection.id, connection.name))
        sockets[connection.id] = connection
        players[connection.id] = player
        sendIndividualMessage(connection.id, S2CEvent.InitializePlayerS2CEvent(player))
        sendIndividualMessage(connection.id, S2CEvent.InitializeOpponentsS2CEvent(players.values.filter { it != player }.map { it.asOpponent() }.toSet()))
        sendIndividualMessage(connection.id, S2CEvent.UpdateRoomSettingsS2CEvent(roomSettings))
        sendIndividualMessage(connection.id, S2CEvent.SyncScoresS2CEvent(previousGameScores))

        broadcast(S2CEvent.OpponentJoinedS2CEvent(player.asOpponent()), exclude = connection)
        updateClientRoom()

        return true
    }

    fun leave(id: Long) {
        val connection = sockets[id] ?: return
        val player = players[id] ?: return

        if (isStarted) {
            deck.cards += player.hand
            player.info.flock.sheep.forEach {
                deck.cards += it.cards
            }
            players[id] = player.copy(hand = emptyList(), info = player.info.copy(flock = Flock()))

            coinFlip?.let {
                if (it.target == id || it.attacker == id) {
                    coinFlip = null
                    broadcast(S2CEvent.CloseCoinFlipS2CEvent)
                }
            }
            pendingGoldCardAction?.let {
                if (it.winner == id) completeGoldCardPendingAction(emptyList(), id)
            }

            if (currentTurnPlayer == id) nextTurn(id)
            broadcast(S2CEvent.NotificationS2CEvent("${player.info.name} Left the game, all their cards in hand and flock have been returned to deck"))
        }

        queue.remove(id)
        players.remove(id)
        sockets.remove(id)

        if (sockets.size == 1) endGame()

        if (sockets.isEmpty()) {
            onDestroy(code)
            return
        } else if (connection == host) {
            changeHost()
        }

        broadcast(S2CEvent.OpponentLeftS2CEvent(player.asOpponent()), exclude = connection)
    }

    fun changeHost() {
        host = sockets.values.random()
        updateClientRoom()
    }

    fun updateRoomSettings(settings: RoomSettings, host: Long) {
        if (host != this.host.id) return
        if (!validateRoomSettings(settings, host)) return
        this.roomSettings = settings

        broadcast(S2CEvent.UpdateRoomSettingsS2CEvent(settings))
    }

    private fun validateRoomSettings(settings: RoomSettings, host: Long): Boolean {
        val message: String = when {
            settings.maxHandSize < 5                        -> "Max hand size cannot be less than 5"
            settings.minHandSize < 1                        -> "Min hand size cannot be less than 1"
            settings.initialHandSize < 1                    -> "Initial hand size cannot be less than 1"
            settings.drawOnEachTurn < 1                     -> "Draw On Each Turn cannot be less than 1"
            settings.rainbowSheepPoints < 1                 -> "Rainbow sheep points cannot be less than 1"
            settings.goldCardPenalty < 0                    -> "Gold card penalty cannot be less than 0"
            settings.minHandSize > settings.maxHandSize     -> "Min hand size cannot be greater than max hand size"
            settings.minHandSize < settings.drawOnEachTurn  -> "Min hand size cannot be less than Draw On Each Turn"
            settings.maxHandSize > Deck.MaxSize             -> "Max hand size cannot be greater than max deck size"
            settings.minHandSize > settings.initialHandSize -> "Min hand size cannot be greater than initial hand size"
            else                                            -> return true
        }

        sendIndividualMessage(host, S2CEvent.NotificationS2CEvent(message))
        return false
    }

    fun updatePlayers() {
        val currentOpponentsMap = sockets.keys.associateWith { currentId ->
            players
                .filterNot { it.key == currentId }
                .map { it.value.asOpponent() }
                .toSet()
        }

        sockets.forEach { (id, connection) ->
            val player = players[id] ?: return@forEach
            val opponents = currentOpponentsMap[id] ?: emptySet()

            connection.sendEvent(S2CEvent.UpdatePlayersS2CEvent(player, opponents, currentTurnPlayer))
        }
        updateClientRoom()
    }

    fun tryStartGame(connection: Connection) {
        if (players[connection.id]?.info?.id != host.id) return
        isStarted = true
        startTime = Clock.System.now().toEpochMilliseconds()
        deck = Deck()
        players.forEach { (id, player) ->
            val drawnCards = getCardsFromDeck(roomSettings.initialHandSize)
            players[id] = player.copy(hand = drawnCards)
            sendGameEvent(GameEvents.DrawCards(eventIds, drawnCards.size, getName(id)).also { card -> cardsLifeCycle[card.id] = drawnCards.map { it.id } })
            queue.add(id)
        }
        nextTurn(currentTurnPlayer)
    }

    fun nextTurn(user: Long) {
        if (!turnCheck(user)) return
        players[currentTurnPlayer]?.let {
            if (it.hand.size > roomSettings.maxHandSize) {
                sendIndividualMessage(it.info.id, S2CEvent.ExceedsMaxHandSizeS2CEvent(it.hand.size - roomSettings.maxHandSize))
                return
            }
        }

        if (queue.isEmpty()) {
            endGame()
            return
        }

        val nextId = queue.removeFirst()
        currentTurnPlayer = nextId
        val nextPlayer = players[nextId] ?: return
        val drawnCards = getCardsFromDeck(maxOf(roomSettings.minHandSize - nextPlayer.hand.size, roomSettings.drawOnEachTurn))
        players[nextId] = nextPlayer.copy(hand = nextPlayer.hand + drawnCards)
        if (drawnCards.isNotEmpty()) queue.add(nextId)
        else {
            if (!finalRoundAnnounced) {
                finalRoundAnnounced = true
                broadcast(S2CEvent.FinalRoundS2CEvent)
            }
            sendIndividualMessage(nextId, S2CEvent.LastTurnS2CEvent)
        }
        sendGameEvent(GameEvents.TurnChange(eventIds, getName(currentTurnPlayer)))
        usedGoldCardForTurn = false
        if (drawnCards.isNotEmpty()) {
            sendGameEvent(GameEvents.DrawCards(eventIds, drawnCards.size, getName(nextId)).also { card -> cardsLifeCycle[card.id] = drawnCards.map { it.id } })
        }
        updatePlayers()
    }

    private fun endGame() {
        val points = players.values.map {
            it.info.name to GameLogic.getPoints(it, roomSettings)
        }.sortedByDescending { it.second }

        isStarted = false
        deck = Deck()
        queue.clear()
        currentTurnPlayer = -1
        coinFlip = null
        pendingGoldCardAction = null
        finalRoundAnnounced = false
        gameEvents.clear()
        cardsLifeCycle.clear()
        eventIds = 0

        players.forEach { (id, player) ->
            players[id] = player.copy(
                info = player.info.copy(flock = Flock()),
                hand = emptyList(),
            )
        }

        previousGameScores += points
        updatePlayers()
        broadcast(S2CEvent.GameOverS2CEvent(points))
    }

    fun getCardsFromDeck(amount: Int): List<Card> {
        val randomCards = deck.cards.shuffled().take(amount)
        deck.cards.removeAll(randomCards)
        return randomCards
    }

    fun turnCheck(user: Long): Boolean {
        return (isStarted && currentTurnPlayer == user).also {
            if (!it) sendIndividualMessage(user, S2CEvent.NotificationS2CEvent("It is not your turn"))
        }
    }

    fun getName(id: Long): String = players[id]?.info?.name ?: "Unknown"

    fun sendGameEvent(event: GameEvents) {
        gameEvents += event
        broadcast(S2CEvent.NotifyGameEventS2CEvent(event))
    }

    fun useWolf(sheep: Sheep, cardId: Int, owner: Long, user: Long) {
        if (!turnCheck(user)) return

        val card = players[user]?.hand?.filterIsInstance<Card.SpecialCard>()?.firstOrNull { it.specialType == SpecialType.Wolf && it.id == cardId } ?: return
        val currentSheep = resolveFlockSheep(owner, sheep) ?: return
        manageSheep(currentSheep, owner = owner, add = false)
        manageCards(card, owner = user, add = false)
        deck.discardPile += currentSheep.cards + card
        sendGameEvent(GameEvents.WheatWolf(eventIds, false, currentSheep.name, getName(user), getName(owner)))
        updatePlayers()
    }

    fun useWheat(sheep: Sheep, cardId: Int, owner: Long, user: Long) {
        if (!turnCheck(user)) return

        val card = players[user]?.hand?.filterIsInstance<Card.SpecialCard>()?.firstOrNull { it.specialType == SpecialType.Wheat && it.id == cardId } ?: return
        val currentSheep = resolveFlockSheep(owner, sheep) ?: return
        manageSheep(currentSheep, owner = owner, add = false)
        manageSheep(currentSheep, owner = user)
        manageCards(card, owner = user, add = false)
        deck.discardPile += card
        sendGameEvent(GameEvents.WheatWolf(eventIds, true, currentSheep.name, getName(user), getName(owner)))
        updatePlayers()
    }

    fun approveCardSelectionRequest(opponentId: Long, cardId: Int, user: Long) {
        if (!turnCheck(user)) return
        val card = players[user]?.hand?.filterIsInstance<Card.SpecialCard>()?.firstOrNull { it.id == cardId && it.specialType == SpecialType.Yoink } ?: return
        val opponent = players[opponentId] ?: return
        val opponentHand = opponent.hand.map { it.id }
        sendIndividualMessage(user, S2CEvent.SelectFromGivenCardsS2CEvent(minOf(2, opponentHand.size), opponentHand, card.id, opponentId))
    }

    fun transferCards(cardIds: List<Int>, cardId: Int, opponentId: Long, user: Long) {
        if (!turnCheck(user)) return
        val cards = players[opponentId]?.hand?.filter { it.id in cardIds } ?: return
        val card = players[user]?.hand?.filterIsInstance<Card.SpecialCard>()?.firstOrNull { it.id == cardId && it.specialType == SpecialType.Yoink } ?: return
        manageCards(*cards.toTypedArray(), owner = opponentId, add = false)
        manageCards(*cards.toTypedArray(), owner = user)
        manageCards(card, owner = user, add = false)
        deck.discardPile += card
        sendGameEvent(GameEvents.YoinkCards(eventIds, cards.size, getName(user), getName(opponentId)).also { yoinkCards ->
            cardsLifeCycle[yoinkCards.id] = cards.map { it.id }
        })
        updatePlayers()
    }

    fun approveCoinFlip(cardId: Int, opponentId: Long, user: Long) {
        if (!turnCheck(user)) return
        if (usedGoldCardForTurn) {
            sendIndividualMessage(user, S2CEvent.NotificationS2CEvent("You have already used a gold card this turn, only 1 gold card can be played per turn."))
            return
        }
        val card = players[user]?.hand?.filterIsInstance<Card.GoldCard>()?.firstOrNull { it.id == cardId } ?: return
        manageCards(card, owner = user, add = false)
        deck.discardPile += card
        val withReflip = hasEligibleReFlipPlayers()
        CoinFlip(
            goldCard = card,
            attacker = user,
            target = opponentId,
            playerChoice = null,
            currentResult = null,
            reFlippable = withReflip.size,
            skippedReFlip = emptyList(),
            closedDialog = emptyList()
        ).let {

            coinFlip = it
            usedGoldCardForTurn = true
            updatePlayers()
            broadcast(S2CEvent.CoinFlipInitiateS2CEvent(it))
            sendGameEvent(GameEvents.PlayGoldCard(eventIds, card.goldCardType, getName(user), getName(opponentId)))
        }
    }

    fun selectCoinFace(isHead: Boolean, user: Long) {
        coinFlip?.let { flip ->
            if (flip.attacker != user) return

            flip.copy(playerChoice = isHead).let {
                coinFlip = it
                broadcast(S2CEvent.UpdateCoinFlipS2CEvent(it))
            }
        }
    }

    fun flipTheCoin(user: Long) {
        coinFlip?.let { flip ->
            if (flip.target != user) return

            val result = Random.nextBoolean()
            val withReflip = hasEligibleReFlipPlayers(emptyList(), flip.closedDialog)
            flip.copy(
                currentResult = result,
                reFlippable = withReflip.size,
                iteration = flip.iteration + 1
            ).let {
                coinFlip = it
                broadcast(S2CEvent.UpdateCoinFlipS2CEvent(it))
                sendGameEvent(GameEvents.CoinFlipResult(eventIds, getName(it.attacker), getName(it.target), it.playerChoice == true, it.currentResult == true))
            }
        }
    }

    fun reFlipCoin(cardId: Int, user: Long) {
        coinFlip?.let { flip ->
            if (flip.currentResult == null || user in flip.skippedReFlip || user in flip.closedDialog) return
            val card = players[user]?.hand?.filterIsInstance<Card.SpecialCard>()?.firstOrNull { it.id == cardId && it.specialType == SpecialType.ReFlip } ?: return
            manageCards(card, owner = user, add = false)
            deck.discardPile += card
            flip.copy(
                reFlippable = hasEligibleReFlipPlayers(emptyList(), flip.closedDialog).size,
                playerChoice = null,
                currentResult = null,
                lastReFlippedBy = user,
                skippedReFlip = emptyList(),
                iteration = flip.iteration + 1
            ).let {
                coinFlip = it
                updatePlayers()
                broadcast(S2CEvent.UpdateCoinFlipS2CEvent(it))
                sendGameEvent(GameEvents.ReFlipped(eventIds, getName(user)))
            }
        }

    }

    fun skipReFlipCoin(permanent: Boolean, user: Long) {
        coinFlip?.let { flip ->
            if (flip.currentResult == null) return
            if (user in flip.closedDialog) return
            if (permanent && user == flip.looser) {
                resolveCoinFlip(flip)
                broadcast(S2CEvent.CloseCoinFlipS2CEvent)
                return
            }

            if (!permanent && !playerHasReFlip(user)) return

            val skippedReFlip = if (!permanent) (flip.skippedReFlip + user).distinct() else flip.skippedReFlip
            val closedDialog = if (permanent) (flip.closedDialog + user).distinct() else flip.closedDialog
            val updatedFlip = flip.copy(
                skippedReFlip = skippedReFlip,
                closedDialog = closedDialog,
                reFlippable = hasEligibleReFlipPlayers(skippedReFlip, closedDialog).size
            )
            if (permanent && updatedFlip.reFlippable == 0) {
                resolveCoinFlip(updatedFlip)
                broadcast(S2CEvent.CloseCoinFlipS2CEvent)
            } else {
                updatedFlip.let {
                    coinFlip = it
                    broadcast(S2CEvent.UpdateCoinFlipS2CEvent(it))
                }
            }
        }
    }

    fun endCoinFlip(user: Long) {
        coinFlip?.let { flip ->
            if (flip.currentResult == null) return
            if (user != flip.winner) return
            if (flip.reFlippable > 2 || (flip.reFlippable == 1 && players[flip.winner]?.hand?.none { it is Card.SpecialCard && it.specialType == SpecialType.ReFlip } == true)) return
            resolveCoinFlip(flip)
        }
    }

    private fun resolveCoinFlip(flip: CoinFlip) {
        broadcast(S2CEvent.CloseCoinFlipS2CEvent)
        when (flip.goldCard.goldCardType) {
            GoldCardType.Lure    -> flockSheepSelection(flip.goldCard, flip.looser, flip.winner, 2)
            GoldCardType.Yoink   -> yoinkEntireHand(flip.looser, flip.winner)
            GoldCardType.Halve   -> flockSheepSelection(flip.goldCard, flip.looser, flip.winner, 2, true)
            GoldCardType.Remove  -> flockSheepSelection(flip.goldCard, flip.looser, flip.winner, 2)
            GoldCardType.Recover -> discardPileSheepSelection(flip.goldCard, flip.looser, flip.winner)
        }
    }

    private fun playerHasReFlip(user: Long): Boolean =
        players[user]?.hand?.any { it is Card.SpecialCard && it.specialType == SpecialType.ReFlip } ?: false

    private fun hasEligibleReFlipPlayers(
        skippedReFlip: List<Long> = coinFlip?.skippedReFlip.orEmpty(),
        closedDialog: List<Long> = coinFlip?.closedDialog.orEmpty(),
    ): Map<Long, Player> =
        players.filter { (id, player) -> id !in skippedReFlip && id !in closedDialog && player.hand.any { it is Card.SpecialCard && it.specialType == SpecialType.ReFlip } }

    private fun yoinkEntireHand(looser: Long, winner: Long) {
        val hand = players[looser]?.hand ?: return
        manageCards(*hand.toTypedArray(), owner = looser, add = false)
        manageCards(*hand.toTypedArray(), owner = winner)
        updatePlayers()
        coinFlip = null
        sendGameEvent(GameEvents.GoldCardResult(eventIds, GoldCardType.Yoink, hand.size, getName(winner), getName(looser)).also { cardResult ->
            cardsLifeCycle[cardResult.id] = hand.map { it.id }
        })
        if (looser == currentTurnPlayer) nextTurn(looser)
    }

    private fun flockSheepSelection(card: Card.GoldCard, looser: Long, winner: Long, amount: Int, selectHalf: Boolean = false) {
        val sheep = players[looser]?.info?.flock?.sheep ?: return
        pendingGoldCardAction = PendingGoldCardAction(card, winner, looser)
        sendIndividualMessage(winner, S2CEvent.SelectFromGivenSheepS2CEvent(amount, sheep, selectHalf))
    }

    private fun discardPileSheepSelection(card: Card.GoldCard, looser: Long, winner: Long) {
        pendingGoldCardAction = PendingGoldCardAction(card, winner, looser)
        val cards = deck.discardPile.filter { it is Card.SheepCard || it is Card.ModifierCard }
        sendIndividualMessage(winner, S2CEvent.SelectSheepFromGivenCardsS2CEvent(cards))
    }

    private fun validateSheepFromCard(cardIds: List<Int>, user: Long) {
        val cards = deck.discardPile.filter { it.id in cardIds }
        if (cards.isEmpty()) {
            completeGoldCardPendingAction(emptyList(), user)
        }
        GameLogic.buildSheep(cards)?.let {
            completeGoldCardPendingAction(listOf(it to null), user)
        }
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
                    sheep.firstOrNull()?.first?.let { recoveredSheep ->
                        deck.discardPile -= recoveredSheep.cards.toSet()
                        manageSheep(recoveredSheep, owner = action.winner)
                    }
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
            sendGameEvent(GameEvents.GoldCardResult(eventIds, action.card.goldCardType, sheep.size, getName(action.winner), getName(action.looser)))
        }
    }

    fun deFrankenSheep(sheep: Sheep, cardId: Int, owner: Long, user: Long) {
        if (!turnCheck(user)) return
        val card = players[user]?.hand?.filterIsInstance<Card.SheepCard>()?.firstOrNull { it.id == cardId } ?: return
        val currentSheep = resolveFlockSheep(owner, sheep) ?: return
        val deFrankenSide = GameLogic.deFrankenReplacement(currentSheep, card) ?: return
        repairSheep(deFrankenSide, currentSheep, card, owner, user)
        sendGameEvent(GameEvents.FixSheep(eventIds, FixSheepType.Franken, currentSheep.name, getName(user), if (owner != user) getName(owner) else null))
        updatePlayers()
    }

    fun dePaintSheep(sheep: Sheep, cardId: Int, owner: Long, user: Long) {
        if (!turnCheck(user)) return
        val card = players[user]?.hand?.filterIsInstance<Card.SheepCard>()?.firstOrNull { it.id == cardId } ?: return
        val currentSheep = resolveFlockSheep(owner, sheep) ?: return
        val dePaintSide = GameLogic.dePaintReplacement(currentSheep, card) ?: return
        repairSheep(dePaintSide, currentSheep, card, owner, user)
        sendGameEvent(GameEvents.FixSheep(eventIds, FixSheepType.Paint, currentSheep.name, getName(user), if (owner != user) getName(owner) else null))
        updatePlayers()
    }

    fun deRainbowSheep(sheep: Sheep, cardId: Int, owner: Long, user: Long) {
        if (!turnCheck(user)) return
        val card = players[user]?.hand?.filterIsInstance<Card.SheepCard>()?.firstOrNull { it.id == cardId } ?: return
        val currentSheep = resolveFlockSheep(owner, sheep) ?: return
        val deRainbowSide = GameLogic.deRainbowReplacement(currentSheep, card) ?: return
        repairSheep(deRainbowSide, currentSheep, card, owner, user)
        sendGameEvent(GameEvents.FixSheep(eventIds, FixSheepType.Rainbow, currentSheep.name, getName(user), if (owner != user) getName(owner) else null))
        updatePlayers()
    }

    private fun resolveFlockSheep(owner: Long, sheep: Sheep): Sheep? {
        val requestedCardIds = sheep.cards.map { it.id }.toSet()
        return players[owner]?.info?.flock?.sheep?.firstOrNull { current ->
            current.cards.map { it.id }.toSet() == requestedCardIds
        }
    }

    private fun repairSheep(side: SheepSide, sheep: Sheep, card: Card.SheepCard, owner: Long, user: Long) {
        val newSheep = when (side) {
            SheepSide.Front -> GameLogic.buildSheep(listOf(sheep.butt, card))
            SheepSide.Back  -> GameLogic.buildSheep(listOf(sheep.head, card))
        } ?: return
        replaceSheep(sheep, newSheep, owner)

        manageCards(card, owner = user, add = false)
        sheep.modifier?.let { manageCards(it, owner = user) }
        when (side) {
            SheepSide.Front -> manageCards(sheep.head, owner = user)
            SheepSide.Back  -> manageCards(sheep.butt, owner = user)
        }
    }

    fun discardCard(cards: List<Card>, user: Long) {
        if (cards.any { it is Card.GoldCard }) {
            sendIndividualMessage(user, S2CEvent.NotificationS2CEvent("You cannot discard gold cards"))
            return
        }
        manageCards(*cards.toTypedArray(), owner = user, add = false)
        deck.discardPile += cards
        sendGameEvent(GameEvents.DiscardedCards(eventIds, cards.size, getName(user)))
        updatePlayers()
    }

    fun playCards(cardIds: List<Int>, user: Long) {
        if (!turnCheck(user)) return

        val cards = players[user]?.hand?.filter { cardIds.contains(it.id) } ?: return
        val sheep = GameLogic.buildSheep(cards) ?: return
        manageSheep(sheep, owner = user)
        manageCards(*cards.toTypedArray(), owner = user, add = false)
        sendGameEvent(GameEvents.PlaceSheep(eventIds, sheep.name, getName(user)))
        updatePlayers()
    }

    fun replaceSheep(sheep: Sheep, newSheep: Sheep, owner: Long) {
        val affectedPlayer = players[owner] ?: return
        val index = affectedPlayer.info.flock.sheep.indexOfFirst { it.id == sheep.id }

        if (index == -1) return

        val newList = affectedPlayer.info.flock.sheep.toMutableList()
        newList[index] = newSheep

        players[owner] = affectedPlayer.copy(
            info = affectedPlayer.info.copy(
                flock = affectedPlayer.info.flock.copy(sheep = newList)
            )
        )
    }

    fun manageSheep(vararg sheep: Sheep, owner: Long, add: Boolean = true) {
        val affectedPlayer = players[owner] ?: return

        val newSheep = if (add) {
            affectedPlayer.info.flock.sheep + sheep
        } else {
            val removedIds = sheep.map { it.id }.toSet()
            affectedPlayer.info.flock.sheep.filterNot { it.id in removedIds }
        }

        players[owner] = affectedPlayer.copy(
            info = affectedPlayer.info.copy(
                flock = affectedPlayer.info.flock.copy(sheep = newSheep)
            )
        )
    }

    fun manageCards(vararg cards: Card, owner: Long, add: Boolean = true) {
        val affectedPlayer = players[owner] ?: return

        val newHand = if (add) {
            affectedPlayer.hand + cards
        } else {
            val removedIds = cards.map { it.id }.toSet()
            affectedPlayer.hand.filterNot { it.id in removedIds }
        }

        players[owner] = affectedPlayer.copy(hand = newHand)
    }

    fun broadcast(event: S2CEvent, exclude: Connection? = null) {
        sockets.forEach { (_, connection) ->
            if (connection != exclude) connection.sendEvent(event)
        }
    }

    fun sendIndividualMessage(id: Long, event: S2CEvent) {
        sockets[id]?.sendEvent(event)
    }

    fun asClientRoom() = ClientRoom(
        code,
        sockets.size,
        players[host.id]?.info ?: PlayerInfo(-1, ""),
        deck.cards.size,
        deck.discardPile.size,
        isStarted,
        Clock.System.now().toEpochMilliseconds() - startTime
    )
}
