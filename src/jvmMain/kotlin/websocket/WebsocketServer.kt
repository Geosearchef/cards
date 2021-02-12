package websocket

import CardSimulatorOptions
import ClientEchoReplyMessage
import ClientLoginMessage
import Message
import ServerEchoRequestMessage
import ServerLoginMessage
import api.Api
import game.GameManager
import game.players.PlayerManager
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.StatusCode
import org.eclipse.jetty.websocket.api.WebSocketException
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import spark.Spark.webSocket
import util.Util
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@WebSocket
object WebsocketServer {
    private val SOCKET_IDLE_TIMEOUT = Duration.ofSeconds(10)
    private val log = Util.logger()

    private val sessions: MutableList<Session> = ArrayList()

    @OnWebSocketConnect
    fun onConnect(session: Session) {
        log.info("Session connected from ${session.getRemoteHostAddress()}") // TODO: switch to debug

        synchronized(sessions) {
            sessions.add(session)
        }
//        session.idleTimeout = SOCKET_IDLE_TIMEOUT.toMillis()
    }

    @OnWebSocketClose
    fun onWebSocketClose(session: Session, statusCode: Int, reason: String) {
        log.info("Session disconnected: ${session.getRemoteHostAddress()} with status code $statusCode, due to $reason")
        disconnect(session);
    }

    val executor = Executors.newScheduledThreadPool(1)
    fun disconnect(session: Session) {
        synchronized(sessions) {
            sessions.remove(session)
        }
        executor.schedule({
            session.disconnect()
        }, 1500, TimeUnit.MILLISECONDS)

        PlayerManager.onSessionDisconnected(session)
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, json: String) {
        val message = Message.fromJson(json) // TODO: test crash

        if(message is ClientLoginMessage) {
            val success = PlayerManager.attemptLogin(message.username, session)
            if(success) {
                send(session, ServerLoginMessage(GameManager.gameInfo, Api.ASSET_TOKEN))
            } else {
                session.close(StatusCode.PROTOCOL, "Username invalid or already taken")
                return
            }
        } else if(message is ClientEchoReplyMessage) {
            PlayerManager.getPlayerBySession(session)?.latency = (System.currentTimeMillis() - message.serverTimestamp).toInt()
        } else {
            val player = PlayerManager.getPlayerBySession(session)
            if(player != null) {
                GameManager.onMessageReceived(message, player)
            } else {
                log.warn("Received message from unauthenticated session ${session.getRemoteHostAddress()}, disconnecting")
                session.close(StatusCode.PROTOCOL, "Unauthenticated")
                return
            }
        }
    }

    fun send(session: Session, message: Message) = send(session, message.toJson())
    fun send(session: Session, message: String): Future<Void> {
        try {
            if(session.isOpen) {
                return session.remote.sendStringByFuture(message) // alternatively bytes
            } else {
                return CompletableFuture.completedFuture(null)
            }
        } catch(e: WebSocketException) {
            log.error("Unable to send to session, disconnecting")
            disconnect(session)
            return CompletableFuture.completedFuture(null)
        }
    }

    object KeepAliveThread {
        val executor = Executors.newScheduledThreadPool(1)
        val task = executor.scheduleAtFixedRate(this::run, 5, 2, TimeUnit.SECONDS)
        fun run() {
            synchronized(sessions) {
                try {
                    ArrayList(sessions).forEach { send(it, ServerEchoRequestMessage(System.currentTimeMillis())) }

                    // time out sessions
                    ArrayList(sessions).filter {
                        PlayerManager.getPlayerBySession(it)?.lastEchoReply?.plus(SOCKET_IDLE_TIMEOUT)?.isBefore(Instant.now()) ?: false
                    }.forEach {
                        log.warn("Force disconnecting ${it.getRemoteHostAddress()}")
                        disconnect(it)
                    }
                } catch(e: Exception) {
                    log.error("Error while monitoring sessions", e)
                }
            }
        }
        fun stop() {
            log.info("Stopping keep alive thread")
            task.cancel(true)
        }
    }

    fun init() {
        webSocket(CardSimulatorOptions.WEBSOCKET_ROUTE, this)
        KeepAliveThread.run()
    }

    fun Session.getRemoteHostAddress() = this.remoteAddress.address.hostAddress
}
