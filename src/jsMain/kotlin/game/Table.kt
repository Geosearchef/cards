package game

import CardSimulatorClient
import ClientCursorPositionMessage
import ClientGameObjectPositionMessage
import game.objects.GameObject
import game.objects.Stack
import game.objects.StackableGameObject
import input.Input
import kotlinx.browser.window
import util.Util
import util.math.Rectangle
import util.math.Vector
import websocket.WebsocketClient
import kotlin.math.pow

object Table {

    private val adjustedGameObjectUpdateInterval: Int get() {
        if(selectedGameObjects.size > 10) {
            return 100
        } else if(selectedGameObjects.size > 30) {
            return 300
        } else {
            return 16
        }
    }

    var offset = Vector(0.0, 0.0)
    var scale = 2.0

    val gameObjects: MutableList<GameObject> = ArrayList()
    val renderedGameObjects get() = gameObjects.filter(GameObject::rendered)

    val selectedGameObjects: MutableList<GameObject> = ArrayList()

    fun onServerGameObjectPosition(gameObject: GameObject, pos: Vector) {
        gameObject.clientExtension.serverPos = pos
        gameObject.lastTouchedOnServer = Util.currentTimeMillis()

        if(gameObject is Stack) {
            gameObject.stackedObjects.forEach { it.clientExtension.serverPos = pos }
        }
    }

    fun onServerStackInfo(stack: Stack, stackedObjects: List<StackableGameObject>) {
        stack.lastTouchedOnServer = Util.currentTimeMillis()
        stack.stackedObjects.forEach { it.stack = null } // remove stack reference -> activate rendering of all objects that might have been removed
        stack.stackedObjects.clear()

        stackedObjects.forEach {
            it.stack = stack
            it.pos = stack.pos
            stack.stackedObjects.add(it)

            selectedGameObjects.remove(it)
            if(Input.grabbedGameObject == it) {
                Input.grabbedGameObject = null
            }
        }
    }



    fun update(delta: Double) {
        gameObjects.forEach { it.clientExtension.update(delta) }
    }

    fun setSelection(rect: Rectangle) {
        selectedGameObjects.clear()

        gameObjects.filter { it.center in rect && (it as? StackableGameObject)?.stack == null }.forEach(selectedGameObjects::add)
    }

    // client sided
    var lastUpdateByGameObject: MutableMap<Long, Long> = HashMap()
    fun onGameObjectMoved(gameObject: GameObject) {


        if(lastUpdateByGameObject[gameObject.id] == null || lastUpdateByGameObject[gameObject.id]!! + adjustedGameObjectUpdateInterval < Util.currentTimeMillis()) {
            transmitGameObjectPosition(gameObject)
        } else {
            val newPos = gameObject.pos
            window.setTimeout({
                if(gameObject.pos == newPos) {
                    transmitGameObjectPosition(gameObject)
                }
            }, adjustedGameObjectUpdateInterval)
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

//                if(target == current) {
//                    return@forEach
//                }
//
//                val toTarget = target - current
//                val targetDistance = toTarget.length()
//                val speed = log2((targetDistance / 30.0) + 1.0) * 800.0
//
//                var change = toTarget.normalise() * speed * delta
//                if(change.length() >= targetDistance) {
//                    change = toTarget
//                }
//
//                renderedCursorPositionByPlayer[it.key] = current + change


                val p = 0.8
                val d = delta * 190.0

                // (p.pow(d) - 1.0) / (p - 1.0) = sum p^(k-1),k=1 to n
                renderedCursorPositionByPlayer[it.key] = (current * p.pow(d)) + (target * (1.0 - p) * (p.pow(d) - 1.0) / (p - 1.0))
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