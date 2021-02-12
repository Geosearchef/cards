package game

import CardSimulatorClient
import ClientJoinSeatMessage
import GameInfo
import Message
import SeatInfo
import ServerAddGameObjectMessage
import ServerCursorPositionMessage
import ServerLoginMessage
import ServerPlayerJoinSeatMessage
import ServerPlayerLeaveSeatMessage
import ServerRemoveGameObjectMessage
import websocket.WebsocketClient

object Game {

    lateinit var gameInfo: GameInfo
    val playersBySeat: MutableMap<Int, String> = HashMap()
    val players: Collection<String> get() = playersBySeat.values

    var ownSeat: Int? = null

    val gameObjects: MutableList<GameObject> = ArrayList()

    fun onServerMessage(msg: Message) {
        when(msg) {
            is ServerLoginMessage -> {
                console.log("Successfully logged in, got game info, table with ${msg.gameInfo.seats.size}")

                gameInfo = msg.gameInfo
                SeatsView.init()
            }

            is ServerPlayerJoinSeatMessage -> {
                playersBySeat[msg.seatId] = msg.playerName

                if(msg.playerName == CardSimulatorClient.username) {
                    ownSeat = msg.seatId
                }

                SeatsView.recreate()
            }

            is ServerPlayerLeaveSeatMessage -> {
                playersBySeat.remove(msg.seatId)
                SeatsView.recreate()
            }

            is ServerCursorPositionMessage -> {
                Table.PlayerCursors.onServerCursorPositionUpdate(msg.playerName, msg.pos)
            }

            is ServerAddGameObjectMessage -> {
                gameObjects.add(msg.gameObject)
                console.log("Got game object: ")
                console.log(msg.gameObject)
            }

            is ServerRemoveGameObjectMessage -> {
                gameObjects.removeAll { it.id == msg.id }
            }

            else -> {
                console.log("Received message of unknown type: ${msg::class}")
            }
        }
    }

    fun onJoinSeatRequest(seatId: Int) {
        WebsocketClient.send(ClientJoinSeatMessage(seatId))
    }

    fun getPlayerColor(playerName: String) : String? = getPlayerSeat(playerName)?.color

    fun getPlayerSeat(playerName: String): SeatInfo? {
        return playersBySeat.entries.find { it.value == playerName }?.let { entry ->
            gameInfo.seats.find { it.id == entry.key }
        }
    }

    fun init() {

    }
}