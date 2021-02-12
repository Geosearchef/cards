import game.GameObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import util.math.Vector

@Serializable
sealed class Message() {
    companion object {
        fun fromJson(json: String) = Json.decodeFromString<Message>(json)
    }
    fun toJson() = Json.encodeToString(this)
}

@Serializable
data class ClientLoginMessage(val username: String) : Message()

@Serializable
data class ServerLoginMessage(val gameInfo: GameInfo, val assetToken: String) : Message()

@Serializable
data class ClientEchoReplyMessage(val serverTimestamp: Long) : Message()

@Serializable
data class ServerEchoRequestMessage(val serverTimestamp: Long) : Message()

@Serializable
data class ClientJoinSeatMessage(val seatId: Int) : Message()

@Serializable
data class ServerPlayerJoinSeatMessage(val playerName: String, val seatId: Int) : Message()
@Serializable
data class ServerPlayerLeaveSeatMessage(val playerName: String, val seatId: Int) : Message()

@Serializable
data class ClientCursorPositionMessage(val pos: Vector) : Message()
@Serializable
data class ServerCursorPositionMessage(val playerName: String, val pos: Vector) : Message()

@Serializable
data class ServerAddGameObjectMessage(val gameObject: GameObject) : Message()

@Serializable
data class ServerRemoveGameObjectMessage(val id: Long) : Message()