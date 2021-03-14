package game

import ClientCursorPositionMessage
import ClientFlipObjectMessage
import ClientGameObjectPositionMessage
import ClientGameObjectReleasedMessage
import ClientJoinSeatMessage
import GameInfo
import Message
import SeatInfo
import ServerAddGameObjectMessage
import ServerPlayerJoinSeatMessage
import ServerPlayerLeaveSeatMessage
import ServerRemoveGameObjectMessage
import ServerSetGameObjectsFlippedMessage
import ServerStackInfoMessage
import game.TaskProcessor.verifyTaskThread
import game.players.Player
import game.players.PlayerManager.broadcast
import game.players.PlayerManager.players
import util.IdFactory
import util.Util.logger
import util.math.Vector

object GameManager {

    const val MAX_STACK_DISTANCE_SQUARED = 10.0 * 10.0

    val log = logger()

    val gameInfo = GameInfo(
        arrayOf(
            SeatInfo(0, "#ED6B4C"),
            SeatInfo(1, "#53C2E0"),
            SeatInfo(2, "#F0C659"),
            SeatInfo(3, "#AB4FF7"),
            SeatInfo(4, "#74F74F"),
        )
    )
    val gameObjects: MutableList<GameObject> = ArrayList()

    // create test objects
    fun init() {
        TaskProcessor.addTask {
            val cardSize = Vector(57.0, 57.0 * (1060.0 / 680.0))  // 57.0 x 88.8

            for(i in 1..13) {
                addGameObject(Card(Vector(i * (cardSize.x + 15.0), 0.0), cardSize, "CardA$i.png", "CardAB.png"))
            }
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
                    gameObjects.find { it.id == msg.id }?.let { player.onGameObjectMoved(it, msg.pos) }
                }
                is ClientFlipObjectMessage -> {
                    gameObjects.filter { msg.objs.contains(it.id) }.forEach { flipGameObject(it, player) }
                }
                is ClientGameObjectReleasedMessage -> {
                    gameObjects.find { it.id == msg.id }?.let { attemptStack(it, msg.pos) }
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
        gameObjects.remove(gameObject)

        broadcast(ServerRemoveGameObjectMessage(gameObject.id))
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

    fun attemptStack(gameObject: GameObject, pos: Vector, bottom: Boolean = false) {
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

    private fun addToStack(stackable: StackableGameObject, stack: Stack, bottom: Boolean = false) {
        verifyTaskThread()

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