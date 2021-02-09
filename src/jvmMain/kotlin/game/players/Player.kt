package game.players

import Message
import ServerCursorPositionMessage
import org.eclipse.jetty.websocket.api.Session
import util.math.Vector
import websocket.WebsocketServer
import java.time.Duration
import java.time.Instant

class Player(val username: String, val session: Session) {

    private val MIN_CURSOR_UPDATE_INTERVAL = Duration.ofMillis(50)

    var latency: Int = Integer.MAX_VALUE
        set(value) {
            field = value
            lastEchoReply = Instant.now()
        }
    var lastEchoReply: Instant = Instant.now()

    var seat: Int? = null
    var cursorPositon: Vector? = null

    var lastCursorUpdate = Instant.now()

    fun send(message: Message) {
        WebsocketServer.send(this.session, message)
    }

    fun updateCursorPosition(pos: Vector) {
        this.cursorPositon = pos
        if (Instant.now().isAfter(lastCursorUpdate.plus(MIN_CURSOR_UPDATE_INTERVAL))) { // limit send rate to 20 Hz
            lastCursorUpdate = Instant.now()
            PlayerManager.broadcast(ServerCursorPositionMessage(username, pos))
        }
    }
}