import api.Api
import game.GameManager
import game.players.PlayerManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import websocket.WebsocketServer
import java.nio.file.Files
import java.nio.file.Paths

@Serializable
data class CardSimulatorServerConfig(val assetsFolder: String, val playerCodes: List<String>, val adminCodes: List<String>)

fun main(args: Array<String>) {
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
    val log = LoggerFactory.getLogger("main")

    val config = Json.decodeFromString<CardSimulatorServerConfig>(String(Files.readAllBytes(Paths.get("cards.json"))))
    Api.ASSETS_FOLDER = Paths.get(config.assetsFolder)
    PlayerManager.PLAYER_CODES = config.playerCodes
    PlayerManager.ADMIN_CODES = config.adminCodes

    log.info("Starting cards server")
    log.info("Ports used: Static: ${CardSimulatorOptions.STATIC_PORT}, API: ${CardSimulatorOptions.API_PORT}, WebSocket: ${CardSimulatorOptions.WEBSOCKET_PORT}")

    WebServer.init()
    WebsocketServer.init()
    Api.init()

    // just serve /static
    // script at /static/output.js

    GameManager.init()
}