package game

import CardSimulatorClient
import ClientCursorPositionMessage
import kotlinx.browser.window
import util.Util
import util.math.Vector
import websocket.WebsocketClient
import kotlin.math.log2

object Table {

    var offset = Vector(0.0, 0.0)
    var scale = 1.0



    object PlayerCursors {
        private const val CURSOR_UPDATE_INTERVAL_MS = 50

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