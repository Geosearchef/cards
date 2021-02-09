package game

import ClientCursorPositionMessage
import ClientJoinSeatMessage
import GameInfo
import Message
import SeatInfo
import ServerPlayerJoinSeatMessage
import ServerPlayerLeaveSeatMessage
import game.TaskProcessor.verifyTaskThread
import game.players.Player
import game.players.PlayerManager.broadcast
import game.players.PlayerManager.players
import util.Util.logger

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

    // single threaded
    fun init() {

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

        players.forEach { player ->
            player.seat?.let { seat ->
                connectingPlayer.send(ServerPlayerJoinSeatMessage(player.username, seat))
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