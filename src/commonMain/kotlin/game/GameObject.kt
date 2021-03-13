package game

import kotlinx.serialization.Serializable
import util.math.Rectangle
import util.math.Vector

@Serializable
sealed class GameObject() {

    var id: Long = -1
    abstract var pos: Vector
    abstract val size: Vector
    abstract val frontAsset: String?
    abstract val backAsset: String?
    var flipped: Boolean = false
    var lastTouchedOnServer = 0L // used for deciding which element is on top

    val aspectRatio: Double get() = size.y / size.x
    val rect: Rectangle get() = Rectangle(pos, size.x, size.y)
    val center: Vector get() = pos + (size / 2.0)

    override fun hashCode(): Int {
        return (id % Int.MAX_VALUE).toInt()
    }

    @kotlinx.serialization.Transient
    abstract val clientExtension: GameObjectClientExtension
}

@Serializable
sealed class StackableGameObject() : GameObject()  {

}

@Serializable
class Card(override var pos: Vector, override val size: Vector, override val frontAsset: String?, override val backAsset: String?) : StackableGameObject()  {

    @kotlinx.serialization.Transient
    override val clientExtension: GameObjectClientExtension = CardClientExtension(this)
}

@Serializable
class Stack(override var pos: Vector, override val size: Vector, override val frontAsset: String?, override val backAsset: String?) : GameObject()  {

    @kotlinx.serialization.Transient
    override val clientExtension: GameObjectClientExtension = StackClientExtension(this)
}