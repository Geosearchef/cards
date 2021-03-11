package game

import CardSimulatorClient
import ClientCursorPositionMessage
import ClientGameObjectPositionMessage
import kotlinx.browser.window
import util.Util
import util.math.Rectangle
import util.math.Vector
import websocket.WebsocketClient
import kotlin.math.log2

object Table {

    private const val GAME_OBJECT_UPDATE_INTERVAL_MS = 16

    var offset = Vector(0.0, 0.0)
    var scale = 2.0

    val gameObjects: MutableList<GameObject> = ArrayList()

    val selectedGameObjects: MutableList<GameObject> = ArrayList()


    fun updateSelection(rect: Rectangle) {
        selectedGameObjects.clear()

        gameObjects.filter { it.center in rect }.forEach(selectedGameObjects::add)
    }


    var lastUpdateByGameObject: MutableMap<Long, Long> = HashMap()
    fun updateGameObjectPosition(gameObject: GameObject) {
        if(lastUpdateByGameObject[gameObject.id] == null || lastUpdateByGameObject[gameObject.id]!! + GAME_OBJECT_UPDATE_INTERVAL_MS < Util.currentTimeMillis()) {
            transmitGameObjectPosition(gameObject)
        } else {
            val newPos = gameObject.pos
            window.setTimeout({
                if(gameObject.pos == newPos) {
                    transmitGameObjectPosition(gameObject)
                }
            }, GAME_OBJECT_UPDATE_INTERVAL_MS)
        }
    }

    fun transmitGameObjectPosition(gameObject: GameObject) {
        WebsocketClient.send(ClientGameObjectPositionMessage(gameObject.pos, gameObject.id))
        lastUpdateByGameObject[gameObject.id] = Util.currentTimeMillis()
    }


    object PlayerCursors {
        private const val CURSOR_UPDATE_INTERVAL_MS = 33

        val cursorPositionByPlayer: MutableMap<String, Vector> = HashMap()
        val renderedCursorPositionByPlayer: MutableMap<String, Vector> = HashMap() // smoothed

        fun onServerCursorPositionUpdate(playerName: String, pos: Vector) {
            cursorPositionByPlayer[playerName] = pos
            CardSimulatorClient.requestRender()
        }

        fun updateCursorPositons(delta: Double) {
            cursorPositionByPlayer.forEach {
                val target = it.value
                val current = renderedCursorPositionByPlayer.getOrPut(it.key, { target })

                if(target == current) {
                    return@forEach
                }

                val toTarget = target - current
                val targetDistance = toTarget.length()
                val speed = log2((targetDistance / 30.0) + 1.0) * 800.0

                var change = toTarget.normalise() * speed * delta
                if(change.length() >= targetDistance) {
                    change = toTarget
                }

                renderedCursorPositionByPlayer[it.key] = current + change
            }
        }


        var currentTransmittedCursorPosition = Vector()
        var lastCursorTransmit = 0L

        fun onCursorMovement(mousePositionTable: Vector) {
            if(Game.ownSeat == null || currentTransmittedCursorPosition == mousePositionTable) {
                return
            }

            if(Util.currentTimeMillis() - lastCursorTransmit < CURSOR_UPDATE_INTERVAL_MS) {
                val cachedLastTransmit = lastCursorTransmit
                window.setTimeout({
                    if(lastCursorTransmit == cachedLastTransmit) {
                        lastCursorTransmit = Util.currentTimeMillis()
                        WebsocketClient.send(ClientCursorPositionMessage(mousePositionTable))
                    }
                }, CURSOR_UPDATE_INTERVAL_MS)
                return
            }

            lastCursorTransmit = Util.currentTimeMillis()
            WebsocketClient.send(ClientCursorPositionMessage(mousePositionTable))
        }
    }
}