package game.objects

open class StackClientExtension(gameObject: GameObject) : GameObjectClientExtension(gameObject) {

    var lastShuffled: Long = 0L

    override fun update(delta: Double) {
        super.update(delta)
    }
}