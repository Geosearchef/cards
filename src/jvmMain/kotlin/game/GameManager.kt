package game

import ClientCursorPositionMessage
import ClientJoinSeatMessage
import GameInfo
import Message
import SeatInfo
import ServerAddGameObjectMessage
import ServerPlayerJoinSeatMessage
import ServerPlayerLeaveSeatMessage
import ServerRemoveGameObjectMessage
import game.TaskProcessor.verifyTaskThread
import game.players.Player
import game.players.PlayerManager.broadcast
import game.players.PlayerManager.players
import util.IdFactory
import util.Util.logger
import util.math.Vector

object GameManager {

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
            val cardSize = Vector(100.0, 100.0 * (1060.0 / 680.0))

            for(i in 0 until 13) {
                addGameObject(Card(Vector(i * (cardSize.x + 30.0), 0.0), cardSize, "CardA$i.png", "CardAB.png"))
            }
        }
    }


    fun addGameObject(gameObject: GameObject) {
        verifyTaskThread()
        gameObject.id = IdFactory.nextGameObjectId++
        gameObjects.add(gameObject)

        broadcast(ServerAddGameObjectMessage(gameObject))
    }

    fun removeGameObject(gameObject: GameObject) {
        verifyTaskThread()
        gameObjects.remove(gameObject)

        broadcast(ServerRemoveGameObjectMessage(gameObject.id))
    }



    fun onMessageReceived(msg: Message, player: Player) {
        TaskProcessor.addTask(player) {
            when (msg) {
                is ClientJoinSeatMessage -> {
                    playerJoinSeat(player, msg.seatId)
                }
                is ClientCursorPositionMessage -> {
                    player.updateCursorPosition(msg.pos)
                }
            }
        }
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