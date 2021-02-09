package game

import ClientJoinSeatMessage
import GameInfo
import Message
import ServerLoginMessage
import ServerPlayerJoinSeatMessage
import ServerPlayerLeaveSeatMessage
import websocket.WebsocketClient

object Game {

    lateinit var gameInfo: GameInfo
    val playersBySeat: MutableMap<Int, String> = HashMap()


    fun onServerMessage(msg: Message) {
        when(msg) {
            is ServerLoginMessage -> {
                console.log("Successfully logged in, got game info, table with ${msg.gameInfo.seats.size}")

                gameInfo = msg.gameInfo
                SeatsView.init()
            }

            is ServerPlayerJoinSeatMessage -> {
                playersBySeat[msg.seatId] = msg.playerName
                SeatsView.recreate()
            }

            is ServerPlayerLeaveSeatMessage -> {
                playersBySeat.remove(msg.seatId)
                SeatsView.recreate()
            }

            else -> {
                console.log("Received message of unknown type: ${msg::class}")
            }
        }
    }




    fun onJoinSeatRequest(seatId: Int) {
        WebsocketClient.send(ClientJoinSeatMessage(seatId))
    }

    fun init() {

    }
}