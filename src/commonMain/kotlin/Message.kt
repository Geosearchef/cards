import game.GameObject
import kotlinx.serialization.SerialName
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
data class ServerLoginMessage(val gameInfo: GameInfo, val assetToken: String, val serverTimestamp: Long) : Message()

@Serializable
data class ClientEchoReplyMessage(val serverTimestamp: Long) : Message()

@Serializable
data class ServerEchoRequestMessage(val serverTimestamp: Long, val lastRTT: Int) : Message()

@Serializable
data class ClientJoinSeatMessage(val seatId: Int) : Message()

@Serializable
data class ServerPlayerJoinSeatMessage(val playerName: String, val seatId: Int) : Message()
@Serializable
data class ServerPlayerLeaveSeatMessage(val playerName: String, val seatId: Int) : Message()

@Serializable @SerialName("clCurPos")
data class ClientCursorPositionMessage(val p: Vector) : Message()
@Serializable @SerialName("seCurPos")
data class ServerCursorPositionMessage(val player: String, val pos: Vector) : Message()

@Serializable @SerialName("seAddObj")
data class ServerAddGameObjectMessage(val gameObject: GameObject) : Message()

@Serializable @SerialName("seDelObj")
data class ServerRemoveGameObjectMessage(val id: Long) : Message()

@Serializable @SerialName("clObjPos")
data class ClientGameObjectPositionMessage(val pos: Vector, val id: Long) : Message()
@Serializable @SerialName("seObjPos")
data class ServerGameObjectPositionMessage(val pos: Vector, val id: Long, val seat: Int) : Message()

@Serializable @SerialName("clFlipObjs")
data class ClientFlipObjectMessage(val objs: Array<Long>) : Message() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return objs.contentEquals((other as ClientFlipObjectMessage).objs)
    }

    override fun hashCode() = objs.contentHashCode()
}

@Serializable @SerialName("seSetFlipObjs")
data class ServerSetGameObjectsFlippedMessage(val objsStatus: Map<Long, Boolean>) : Message()

@Serializable @SerialName("clDropObj")
data class ClientGameObjectReleasedMessage(val pos: Vector, val id: Long) : Message() // position not used for setting, just for stack checking