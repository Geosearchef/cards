import game.objects.GameObject
import kotlinx.serialization.SerialName
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
data class ClientLoginMessage(val username: String, val code: String) : Message()

@Serializable
data class ServerLoginMessage(val gameInfo: GameInfo, val assetToken: String, val serverTimestamp: Long, val admin: Boolean) : Message()

@Serializable @SerialName("clEchoRep")
data class ClientEchoReplyMessage(val serverTimestamp: Long) : Message()

@Serializable @SerialName("seEchoReq")
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
data class ClientGameObjectPositionMessage(val pos: Vector, val id: Long, @SerialName("sel") val selectedObjects: Int) : Message()
@Serializable @SerialName("seObjPos")
data class ServerGameObjectPositionMessage(val pos: Vector, val id: Long, val seat: Int) : Message()

@Serializable @SerialName("clFlipObjs")
data class ClientFlipObjectMessage(val objs: Array<Long>) : Message()

@Serializable @SerialName("seSetFlipObjs")
data class ServerSetGameObjectsFlippedMessage(val objsStatus: Map<Long, Boolean>) : Message()

@Serializable @SerialName("clDropObj")
data class ClientGameObjectReleasedMessage(val pos: Vector, val id: Long) : Message() // position not used for setting, just for stack checking

@Serializable @SerialName("seStackInfo")
data class ServerStackInfoMessage(val id: Long, val stackedObjects: List<Long>) : Message()
@Serializable @SerialName("clUnstackObj")
data class ClientUnstackGameObjectMessage(val id: Long) : Message()

@Serializable @SerialName("clGroupObjs")
data class ClientGroupObjectsMessage(val objs: Array<Long>) : Message()
@Serializable @SerialName("clShuffleStacks")
data class ClientShuffleStacksMessage(val objs: Array<Long>) : Message()
@Serializable @SerialName("clDealStack")
data class ClientDealStackMessage(val stackId: Long) : Message()
@Serializable @SerialName("clSortPlayerZone")
class ClientSortPlayerZoneMessage() : Message()

@Serializable @SerialName("clDeleteObj")
data class ClientAdminDeleteGameObjectsMessage(val objs: List<Long>) : Message()
@Serializable @SerialName("clDeleteAll")
class ClientAdminDeleteAllGameObjectsMessage() : Message()

@Serializable
data class ClientAdminSpawnDeckMessage(val deck: String) : Message()

@Serializable
data class ClientPlayerNoteUpdateMessage(val note: String) : Message()
@Serializable
data class ServerPlayerNoteUpdateMessage(val note: String, val seatId: Int) : Message()

@Serializable
data class ClientPublicNoteUpdateMessage(val note: String) : Message()
@Serializable
data class ServerPublicNoteUpdateMessage(val note: String, val sourceSeat: Int) : Message()

@Serializable
data class ServerStackShuffledInfoMessage(val stackId: Long) : Message()