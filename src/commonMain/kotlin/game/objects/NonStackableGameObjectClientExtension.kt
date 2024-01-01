package game.objects

class NonStackableGameObjectClientExtension(gameObject: GameObject) : GameObjectClientExtension(gameObject) {
    override fun update(delta: Double) {
        super.update(delta)
    }
}