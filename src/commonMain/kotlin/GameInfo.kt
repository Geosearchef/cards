import game.objects.GameObject
import kotlinx.serialization.Serializable
import util.math.Rectangle
import util.math.Vector

@Serializable
data class GameInfo(val seats: Array<SeatInfo>, val playerZones: Array<PlayerZone>)
@Serializable
data class SeatInfo(val id: Int, val color: String)
@Serializable
data class PlayerZone(val seatId: Int, val rect: Rectangle) {
    operator fun contains(vector: Vector) = vector in rect
    operator fun contains(gameObject: GameObject) = gameObject.center in this
}