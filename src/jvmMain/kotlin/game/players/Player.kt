package game.players

import Message
import ServerCursorPositionMessage
import ServerGameObjectPositionMessage
import game.GameManager
import game.objects.GameObject
import game.objects.Stack
import org.eclipse.jetty.websocket.api.Session
import util.math.Vector
import websocket.WebsocketServer
import java.time.Duration
import java.time.Instant

class Player(val username: String, val admin: Boolean, val session: Session) {

    private val MIN_CURSOR_UPDATE_INTERVAL = Duration.ofMillis(16)
    private val MIN_OBJECT_UPDATE_INTERVAL = Duration.ofMillis(12)

    var latency: Int = Integer.MAX_VALUE
        set(value) {
            field = value
            lastEchoReply = Instant.now()
        }
    var lastEchoReply: Instant = Instant.now()

    var seat: Int? = null
    var cursorPositon: Vector? = null

    var lastCursorUpdate = Instant.now()
    var lastUpdateByGameObject: MutableMap<Long, Instant> = HashMap() // leaks ids of deleted gameObjects

    fun send(message: Message) {
        WebsocketServer.send(this.session, message)
    }

    fun updateCursorPosition(pos: Vector) {
        this.cursorPositon = pos
        if (Instant.now().isAfter(lastCursorUpdate.plus(MIN_CURSOR_UPDATE_INTERVAL))) { // limit send rate to 60 Hz
            lastCursorUpdate = Instant.now()
            PlayerManager.broadcast(ServerCursorPositionMessage(username, pos))
        }
    }

    fun onGameObjectMoved(gameObject: GameObject, newPos: Vector, checkPlayerHands: Boolean) {
        if(seat == null) {
            return
        }

        // rate limit only, no retransmit
        if(!lastUpdateByGameObject.containsKey(gameObject.id) || Instant.now().isAfter(lastUpdateByGameObject[gameObject.id]!!.plus(MIN_OBJECT_UPDATE_INTERVAL))) {
            GameManager.setGameObjectPos(gameObject, newPos, checkPlayerHands)

            if(GameManager.gameInfo.playerZones.none {gameObject in it} || !checkPlayerHands) {
                PlayerManager.broadcast(ServerGameObjectPositionMessage(gameObject.pos, gameObject.id, seat!!)) // avoid moving card back to top
            }
            lastUpdateByGameObject[gameObject.id] = Instant.now()

            if(gameObject is Stack) {
                gameObject.stackedObjects.forEach { it.pos = gameObject.pos } // not sent, done on client as well
            }
        } else {
//            val oldPos = gameObject.pos
//            executor.schedule({
//                TaskProcessor.addTask {
//                    if(gameObject.pos == oldPos) {
//                        gameObject.pos = newPos
//                        PlayerManager.broadcast(ServerGameObjectPositionMessage(newPos, gameObject.id, seat!!))
//                    }
//                }
//            }, MIN_OBJECT_UPDATE_INTERVAL.toMillis(), TimeUnit.MILLISECONDS)
        }
    }
}