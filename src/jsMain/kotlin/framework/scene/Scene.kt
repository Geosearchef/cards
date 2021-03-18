package framework.scene

import org.w3c.dom.CanvasRenderingContext2D

open class Scene(val input: SceneInput, val update: SceneUpdate, val renderer: SceneRenderer, val uiManager: UIManager, val initFunction: () -> Unit) {

    init {
        @Suppress("LeakingThis")
        SceneManager.register(this)
    }

    interface SceneUpdate {
        fun update(delta: Double)
    }
    interface SceneRenderer {
        fun render(ctx: CanvasRenderingContext2D, width: Int, height: Int)
    }
}