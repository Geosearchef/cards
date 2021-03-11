package websocket

import CardSimulatorOptions
import ClientEchoReplyMessage
import ClientLoginMessage
import Message
import ServerEchoRequestMessage
import game.Game
import kotlinx.browser.window
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.dom.url.URLSearchParams

object WebsocketClient {

    lateinit var socket: WebSocket

    // stats
    var transmittedMessages = 0
    var receivedMessages = 0
    var transmittedBytes = 0
    var receivedBytes = 0

    fun send(message: Message) {
        val json = message.toJson()
        socket.send(json)

        transmittedMessages++
        transmittedBytes += json.length
    }

    fun onSocketMessage(event: MessageEvent) {
        val message = Message.fromJson(event.data.toString())

        receivedMessages++
        receivedBytes += event.data.toString().length

        if(message is ServerEchoRequestMessage) {
            send(ClientEchoReplyMessage(message.serverTimestamp))
            return
        }

        Game.onServerMessage(message)
    }

    fun onSocketClose() {
        console.log("Connection to websocket lost")
        window.location.href = "/?connectionLost=true";
    }

    fun onSocketOpen() {
        console.log("Connected to websocket, logging in...")
        val username = URLSearchParams(window.location.search).get("username") // TODO: put into central location (game / state)
        username?.let { send(ClientLoginMessage(username)) }
    }

    fun init() {
        val websocketUrl = "ws://${window.location.hostname}:${window.location.port}${CardSimulatorOptions.WEBSOCKET_ROUTE}"
        console.log("Connecting to web socket at $websocketUrl")
        socket = WebSocket(websocketUrl)

        socket.onopen = { onSocketOpen() }
        socket.onclose = { onSocketClose() }
        socket.onmessage = { event: MessageEvent -> onSocketMessage(event) }
        socket.onerror = { console.error("Websocket error: ", it) }
    }
}