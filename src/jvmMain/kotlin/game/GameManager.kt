package game

import ClientAdminDeleteAllGameObjectsMessage
import ClientAdminDeleteGameObjectsMessage
import ClientAdminSpawnDeckMessage
import ClientCursorPositionMessage
import ClientDealStackMessage
import ClientFlipObjectMessage
import ClientGameObjectPositionMessage
import ClientGameObjectReleasedMessage
import ClientGroupObjectsMessage
import ClientJoinSeatMessage
import ClientPlayerNoteUpdateMessage
import ClientPublicNoteUpdateMessage
import ClientShuffleStacksMessage
import ClientSortPlayerZoneMessage
import ClientUnstackGameObjectMessage
import GameInfo
import Message
import PlayerZone
import SeatInfo
import ServerAddGameObjectMessage
import ServerGameObjectPositionMessage
import ServerPlayerJoinSeatMessage
import ServerPlayerLeaveSeatMessage
import ServerPlayerNoteUpdateMessage
import ServerPublicNoteUpdateMessage
import ServerRemoveGameObjectMessage
import ServerSetGameObjectsFlippedMessage
import ServerStackInfoMessage
import ServerStackShuffledInfoMessage
import game.TaskProcessor.verifyTaskThread
import game.objects.GameObject
import game.objects.Stack
import game.objects.StackableGameObject
import game.players.Player
import game.players.PlayerManager.broadcast
import game.players.PlayerManager.players
import util.IdFactory
import util.Util
import util.Util.logger
import util.math.Rectangle
import util.math.Vector
import websocket.WebsocketServer.getRemoteHostAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object GameManager {

    const val MAX_STACK_DISTANCE_SQUARED = 10.0 * 10.0
    const val PLAYER_ZONE_PADDING = 28.0
    const val PLAYER_ZONE_CARD_PADDING = 6.0
    const val MAX_SELECTED_OBJECTS_TO_CHECK_PLAYER_HAND = 10

    val log = logger()
    val executor = Executors.newSingleThreadScheduledExecutor()

    val gameInfo = GameInfo(
        arrayOf(
            SeatInfo(0, "#ED6B4C"),
            SeatInfo(1, "#53C2E0"),
            SeatInfo(2, "#F0C659"),
            SeatInfo(3, "#AB4FF7"),
            SeatInfo(4, "#74F74F"),
        ),
        arrayOf(
            PlayerZone(0, Rectangle(-575.0, 230.0, 350.0, 140.0)),
            PlayerZone(1, Rectangle(-175.0, 230.0, 350.0, 140.0)),
            PlayerZone(2, Rectangle(225.0, 230.0, 350.0, 140.0)),
            PlayerZone(3, Rectangle(-575.0, -330.0, 350.0, 140.0)),
            PlayerZone(4, Rectangle(-175.0, -330.0, 350.0, 140.0)),
        ),
        Deck.values().map { it.identifier }
    )
    val gameObjects: MutableList<GameObject> = ArrayList()
    val playerNotesBySeat: MutableMap<Int, String> = HashMap()
    var publicNote: String = ""

    // create test objects
    fun init() {
        TaskProcessor.addTask {
            Deck.DEMO.spawn()
        }
    }

    fun onMessageReceived(msg: Message, player: Player) {
        TaskProcessor.addTask(player) {
            when (msg) {
                is ClientJoinSeatMessage -> {
                    playerJoinSeat(player, msg.seatId)
                }
            }

            if(player.seat == null) {
                return@addTask
            }

            when(msg) {
                is ClientCursorPositionMessage -> {
                    player.updateCursorPosition(msg.p)
                }
                is ClientGameObjectPositionMessage -> {
                    gameObjects.find { it.id == msg.id }?.let { player.onGameObjectMoved(it, msg.pos, checkPlayerHands = msg.selectedObjects <= MAX_SELECTED_OBJECTS_TO_CHECK_PLAYER_HAND) }
                }
                is ClientFlipObjectMessage -> {
                    gameObjects.filter { msg.objs.contains(it.id) }.forEach { flipGameObject(it, player) }
                }
                is ClientGameObjectReleasedMessage -> {
                    gameObjects.find { it.id == msg.id }?.let { attemptStack(it/*, msg.pos*/) }
                }
                is ClientUnstackGameObjectMessage -> {
                    gameObjects.filterIsInstance<StackableGameObject>().find { it.id == msg.id}?.let {
                        if(it.stack?.topObject != it) {
                            log.warn("${player.username} tried to unstack gameobject from the middle of a stack, blocking...")
                        } else {
                            removeFromStack(it)
                        }
                    }
                }
                is ClientGroupObjectsMessage -> {
                    groupGameObjects(gameObjects.filter { msg.objs.contains(it.id) }.filterIsInstance<StackableGameObject>().filter { it.stack == null })
                }
                is ClientShuffleStacksMessage -> {
                    gameObjects.filter { msg.objs.contains(it.id) }.filterIsInstance<Stack>().forEach { shuffleStack(it) }
                }
                is ClientDealStackMessage -> {
                    (gameObjects.find { it.id == msg.stackId } as? Stack)?.let {
                        dealFromStack(it)
                    }
                }
                is ClientSortPlayerZoneMessage -> {
                    player.seat?.let { seat -> alignGameObjectsIntoPlayerZone(gameInfo.playerZones[seat], sortById = true) }
                }
                is ClientPlayerNoteUpdateMessage -> {
                    player.seat?.let { seat ->
                        playerNotesBySeat[seat] = msg.note
                        broadcast(ServerPlayerNoteUpdateMessage(msg.note, seat))
                    }
                }
                is ClientPublicNoteUpdateMessage -> {
                    player.seat?.let { seat ->
                        publicNote = msg.note
                        broadcast(ServerPublicNoteUpdateMessage(msg.note, seat))
                    }
                }
            }

            if(! player.admin) {
                return@addTask
            }

            when(msg) {
                is ClientAdminDeleteGameObjectsMessage -> {
                    println("As requested by ${player.username} from ${player.session.getRemoteHostAddress()}, deleting game objects: ${msg.objs}")
                    gameObjects.filter { msg.objs.contains(it.id) }.forEach { removeGameObject(it) }
                }
                is ClientAdminDeleteAllGameObjectsMessage -> {
                    println("DELETING ALL GAME OBJECTS as requested by ${player.username} from ${player.session.getRemoteHostAddress()}")
                    ArrayList(gameObjects).forEach { removeGameObject(it) }
                }
                is ClientAdminSpawnDeckMessage -> {
                    println("Spawning deck ${msg.deck} as requested by ${player.username}")
                    Deck.values().find { it.identifier == msg.deck }?.spawn()
                }
            }
        }
    }

    fun addGameObject(gameObject: GameObject) {
        verifyTaskThread()
        gameObject.id = IdFactory.nextGameObjectId++
        gameObject.lastTouchedOnServer = System.currentTimeMillis()
        gameObjects.add(gameObject)

        broadcast(ServerAddGameObjectMessage(gameObject))
    }

    fun removeGameObject(gameObject: GameObject) {
        verifyTaskThread()

        if(! gameObjects.contains(gameObject)) {
            return // might happen due to recursive calls (stack already removed contaning the element)
        }

        gameObjects.remove(gameObject)

        // remove from stacks
        gameObjects.filterIsInstance<Stack>().filter { it.stackedObjects.contains(gameObject) }.forEach {
            removeFromStack(gameObject as StackableGameObject)
        }

        // remove contained objects if stack
        if(gameObject is Stack) {
            gameObject.stackedObjects.forEach {
                it.stack = null // prevent stack removal and therefore broadcast, we are removing the stack anyways
                removeGameObject(it)
            }
        }

        broadcast(ServerRemoveGameObjectMessage(gameObject.id))
    }

    fun setGameObjectPos(gameObject: GameObject, newPos: Vector, checkPlayerHands: Boolean = true) {
        val oldPos = gameObject.pos.clone()
        gameObject.pos = newPos
        gameObject.lastTouchedOnServer = System.currentTimeMillis()

        if(checkPlayerHands) { // may be disabled if moving to many cards
            gameInfo.playerZones.find { gameObject in it || oldPos in it }?.let { alignGameObjectsIntoPlayerZone(it) }
        }
    }

    fun alignGameObjectsIntoPlayerZone(zone: PlayerZone, sortById: Boolean = false) {
        val gameObjectsInZoneUnsorted = gameObjects.filter { (it as? StackableGameObject)?.stack == null }.filter { it in zone }
        val gameObjectsInZone = gameObjectsInZoneUnsorted.sortedBy { if(sortById) it.id.toDouble() else it.pos.x }
        if(gameObjectsInZone.isEmpty()) {
            return
        }

        val availableWidth = zone.rect.width - (PLAYER_ZONE_PADDING * 2.0)
        val neededWidth = gameObjectsInZone.sumByDouble { it.rect.width }
        val percentageGranted = availableWidth / neededWidth

        val overshootOfLastObject = gameObjectsInZone.last().rect.width * (1.0 - percentageGranted)
        var overshootCorrectionPerObject = overshootOfLastObject / gameObjectsInZone.size

        var offset = 0.0
        if(percentageGranted > 1.0) {
            overshootCorrectionPerObject = 0.0
            offset = (availableWidth - (neededWidth + PLAYER_ZONE_CARD_PADDING * gameObjectsInZone.size)) / 2.0
        }

        var currentX = zone.rect.x + PLAYER_ZONE_PADDING - overshootCorrectionPerObject + offset + (if(percentageGranted > 1.0) PLAYER_ZONE_CARD_PADDING / 2.0 else 0.0)
        gameObjectsInZone.forEach {
            it.pos = Vector(currentX, zone.rect.center.y - (it.rect.height / 2.0))
            if(percentageGranted < 1.0) {
                currentX += it.rect.width * percentageGranted - overshootCorrectionPerObject
            } else {
                currentX += it.rect.width + PLAYER_ZONE_CARD_PADDING
            }
            broadcast(ServerGameObjectPositionMessage(it.pos, it.id, zone.seatId))
        }
    }

    fun flipGameObject(gameObject: GameObject, source: Player?) {
        verifyTaskThread()
        gameObject.lastTouchedOnServer = System.currentTimeMillis()

        if(gameObject is Stack) {
            gameObject.stackedObjects.forEach { flipGameObject(it, source) }
            gameObject.stackedObjects.reverse()
            broadcastStack(gameObject)
        } else {
            gameObject.flipped = !gameObject.flipped
            broadcast(ServerSetGameObjectsFlippedMessage(mapOf(gameObject.id to gameObject.flipped)))
        }
    }

    fun attemptStack(gameObject: GameObject, /*pos: Vector, */bottom: Boolean = false) {
        verifyTaskThread()

//        if(gameObject is Stack) {
//            gameObjects.filterIsInstance<StackableGameObject>()
//                .filter { it.stack == null }
//                .filter { (it.pos - gameObject.pos).lengthSquared() < MAX_STACK_DISTANCE_SQUARED }
//                .forEach { attemptStack(it, it.pos, bottom = true) }
//            return
//        }

        if(gameObject !is StackableGameObject && gameObject !is Stack) {
            return
        }

        var baseStack: Stack? = gameObjects
            .filterIsInstance<Stack>()
            .filter { it != gameObject }
            .find { (it.pos - gameObject.pos).lengthSquared() < MAX_STACK_DISTANCE_SQUARED }

        if(baseStack == null) {
            var other = gameObjects
                .filterIsInstance<StackableGameObject>()
                .filter { it.stack == null }
                .filter { it != gameObject }
                .find { (it.pos - gameObject.pos).lengthSquared() < MAX_STACK_DISTANCE_SQUARED }

            other?.let {
                val newStack = Stack(other.pos.clone(), other.size.clone(), null, null)
                addGameObject(newStack)
                addToStack(it, newStack)

                baseStack = newStack
            }
        }

        baseStack?.let { baseStack ->
            if(gameObject is StackableGameObject) {
                addToStack(gameObject, baseStack, bottom)
            } else if(gameObject is Stack) {
                for (it in ArrayList(gameObject.stackedObjects)) {
                    val flipStatus = it.flipped
                    broadcast(ServerSetGameObjectsFlippedMessage(mapOf(it.id to true))) // TODO: this is a bad workaround
                    removeFromStack(it)
                    addToStack(it, baseStack)
                    broadcast(ServerSetGameObjectsFlippedMessage(mapOf(it.id to flipStatus)))
                }
            }
        }
    }

    private fun groupGameObjects(objects: List<StackableGameObject>) {
        verifyTaskThread()

        if(objects.size < 2) {
            return
        }

        val center = objects.map { it.pos }.reduce { acc, pos -> acc + pos } / objects.size.toDouble()
        val stack = Stack(center, objects.last().size.clone(), null, null)
        addGameObject(stack)

        // could be done in one go with one update, would increase the amount of code, reduce network traffic
        objects.forEach { addToStack(it, stack) }
    }

    private fun shuffleStack(stack: Stack) {
        verifyTaskThread()

        Util.shuffleListInPlace(stack.stackedObjects)
        broadcastStack(stack)
        broadcast(ServerStackShuffledInfoMessage(stack.id))
    }

    private fun addToStack(stackable: StackableGameObject, stack: Stack, bottom: Boolean = false) {
        verifyTaskThread()

        stackable.stack?.let {
//            println("Couldn't add object ${stackable.id} to stack ${stack.id} as it is already in stack ${stackable.stack?.id}")
            println("Object ${stackable.id} already in stack ${it.id}, removing to add to ${stack.id}")
            stackable.stack = null
            it.stackedObjects.remove(stackable)
            broadcastStack(it)
        }

        if(bottom) {
            stack.stackedObjects.add(0, stackable)
        } else {
            stack.stackedObjects.add(stackable)
        }

        stackable.stack = stack
        stackable.pos = stack.pos.clone()

        broadcastStack(stack)
    }

    private fun removeFromStack(stackable: StackableGameObject) {
        verifyTaskThread()

        stackable.stack?.let { stack ->
            stack.stackedObjects.remove(stackable)

            broadcastStack(stack)

            if(stack.stackedObjects.isEmpty()) {
                removeGameObject(stack)
            }
        }
        stackable.stack = null
    }

    private fun broadcastStack(stack: Stack) {
        broadcast(ServerStackInfoMessage(stack.id, stack.stackedObjects.map { it.id }))
    }

    private fun dealFromStack(stack: Stack) {
        players.filter { it.seat != null }.forEach { player ->
            gameInfo.playerZones.find { it.seatId == player.seat }?.let { playerZone ->
                stack.topObject?.let { dealtObj ->
                    removeFromStack(dealtObj)
                    setGameObjectPos(
                        dealtObj,
                        playerZone.rect.pos + Vector(
                            playerZone.rect.width - 10.0 - dealtObj.rect.width / 2.0,
                            playerZone.rect.height / 2.0 - dealtObj.rect.height / 2.0
                        )
                    )

                    executor.schedule({
                        TaskProcessor.addTask {
                            if(dealtObj.flipped) {
                                flipGameObject(dealtObj, null)
                            }
                        }
                    }, 500, TimeUnit.MILLISECONDS)
                }
            }
        }
        broadcastStack(stack)
    }


    fun onPlayerInitialConnect(connectingPlayer: Player) {
        verifyTaskThread()

        // send seated players
        players.forEach { player ->
            player.seat?.let { seat ->
                connectingPlayer.send(ServerPlayerJoinSeatMessage(player.username, seat))
            }
        }

        // send game objects
        gameObjects.forEach {
            connectingPlayer.send(ServerAddGameObjectMessage(it))
            if(it is Stack) {
                connectingPlayer.send(ServerStackInfoMessage(it.id, it.stackedObjects.map { it.id }))
            }
        }

        // send player notes
        playerNotesBySeat.entries.forEach {
            connectingPlayer.send(ServerPlayerNoteUpdateMessage(it.value, it.key))
        }

        // send public note
        broadcast(ServerPublicNoteUpdateMessage(publicNote, -1))
    }

    fun playerJoinSeat(player: Player, seatId: Int) {
        verifyTaskThread()

        if (gameInfo.seats.none { it.id == seatId }) {
            return
        }

        playerLeaveSeat(player)

        if (players.none { it.seat == seatId }) {
            player.seat = seatId
            broadcast(ServerPlayerJoinSeatMessage(player.username, seatId))
        }
    }

    fun playerLeaveSeat(player: Player) {
        verifyTaskThread()

        player.seat?.let { seat ->
            broadcast(ServerPlayerLeaveSeatMessage(player.username, seat))

            player.seat = null
        }
    }
}