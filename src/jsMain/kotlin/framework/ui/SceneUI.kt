package framework.ui

import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.events.Event
import util.math.Vector

abstract class SceneUI(val width: Int, val height: Int) {

    private val components = ArrayList<UIComponent>()

    open fun render(ctx: CanvasRenderingContext2D) {
        components.forEach { it.render(ctx) }
    }

    fun addComponent(component: UIComponent) {
        components.add(component)
    }

    fun removeComponent(component: UIComponent) {
        components.remove(component)
    }

    open fun onUIPressed(mousePosition: Vector, event: Event) {

    }
    abstract fun isMouseEventOnUI(mousePosition: Vector): Boolean
}