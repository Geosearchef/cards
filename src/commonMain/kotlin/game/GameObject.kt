package game

import kotlinx.serialization.Serializable
import util.math.Vector

@Serializable
sealed class GameObject() {

    var id: Long = -1
    abstract val pos: Vector
    abstract val size: Vector
    abstract val frontAsset: String?
    abstract val backAsset: String?
    var flipped: Boolean = false

    val aspectRatio: Double get() = size.y / size.x
}

@Serializable
sealed class StackableGameObject() : GameObject()  {

}

@Serializable
class Card(override val pos: Vector, override val size: Vector, override val frontAsset: String?, override val backAsset: String?) : StackableGameObject()  {

}

@Serializable
class Stack(override val pos: Vector, override val size: Vector, override val frontAsset: String?, override val backAsset: String?) : GameObject()  {

}