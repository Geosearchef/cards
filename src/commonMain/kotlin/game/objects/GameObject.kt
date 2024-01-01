package game.objects

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

    open val rendered: Boolean get() = true
    open fun getUsedAsset(inOtherPlayerZone: Boolean = false): String? = if (flipped || inOtherPlayerZone) backAsset else frontAsset

//    @kotlinx.serialization.Transient (is already abstract)
    abstract val clientExtension: GameObjectClientExtension
}

@Serializable
class NonStackableGameObject(override var pos: Vector, override val size: Vector, override val frontAsset: String?, override val backAsset: String?) : GameObject() {
    @kotlinx.serialization.Transient
    override val clientExtension: NonStackableGameObjectClientExtension = NonStackableGameObjectClientExtension(this)
}

@Serializable
sealed class StackableGameObject() : GameObject()  {

    var stack: Stack? = null // TODO: remove from selected game objects on stack

    override val rendered get() = stack == null
}

@Serializable
class Card(override var pos: Vector, override val size: Vector, override val frontAsset: String?, override val backAsset: String?) : StackableGameObject()  {

    @kotlinx.serialization.Transient
    override val clientExtension: CardClientExtension = CardClientExtension(this)
}

@Serializable
class Stack(override var pos: Vector, override val size: Vector, override val frontAsset: String?, override val backAsset: String?) : GameObject()  {

    @kotlinx.serialization.Transient
    var stackedObjects: MutableList<StackableGameObject> = ArrayList() // last element is on top
    val topObject get() = stackedObjects.lastOrNull()

    override fun getUsedAsset(inOtherPlayerZone: Boolean): String? = stackedObjects.lastOrNull()?.getUsedAsset(inOtherPlayerZone)

    @kotlinx.serialization.Transient
    override val clientExtension: StackClientExtension = StackClientExtension(this)
}

@Serializable
class TokenObject(override var pos: Vector, override val size: Vector, val color: String) : StackableGameObject()  {

    override val frontAsset: String? = null
    override val backAsset: String? = null

    @kotlinx.serialization.Transient
    override val clientExtension: TokenObjectClientExtension = TokenObjectClientExtension(this)
}