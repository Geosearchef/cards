package update

import framework.scene.Scene
import game.Table

object Update : Scene.SceneUpdate {
    override fun update(delta: Double) {
        Table.PlayerCursors.updateCursorPositons(delta)
    }
}