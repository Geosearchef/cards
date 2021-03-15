package framework.scene

import org.w3c.dom.TouchEvent
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.get
import util.math.Vector

abstract class SceneInput {
    var mousePosition: Vector = Vector()
    var mouseMovement: Vector = Vector()
    var isOnUI: Boolean = false

    fun sealedOnMouseMove(event: MouseEvent) {
        mousePosition = Vector(event.offsetX, event.offsetY)
        mouseMovement = Vector(js("event.movementX") as Double, js("event.movementY") as Double) // not supported by internet explorer, therefore not exposed
        isOnUI = SceneManager.currentScene.uiManager.getUI().isMouseEventOnUI(mousePosition)
        onMouseMove(event, isOnUI)
    }
    fun sealedOnMouseDown(event: MouseEvent) {
        mousePosition = Vector(event.offsetX, event.offsetY)
        onMouseDown(event, isOnUI)

        if(isOnUI) {
            SceneManager.currentScene.uiManager.getUI().onUIPressed(mousePosition, event)
        }
    }
    fun sealedOnMouseUp(event: MouseEvent) {
        mousePosition = Vector(event.offsetX, event.offsetY)
        onMouseUp(event, isOnUI)
    }
    fun sealedOnMouseWheel(event: WheelEvent) {
        onMouseWheel(event, isOnUI)
    }
    fun sealedOnTouchMove(event: TouchEvent) {
        val oldMousePos = mousePosition.clone()
        mousePosition = Vector(event.touches.get(0)?.clientX?.toDouble() ?: 0.0, event.touches.get(0)?.clientY?.toDouble() ?: 0.0)

        mouseMovement = mousePosition - oldMousePos
        isOnUI = SceneManager.currentScene.uiManager.getUI().isMouseEventOnUI(mousePosition)
        onTouchMove(event, isOnUI)
    }
    fun sealedOnTouchStart(event: TouchEvent) {
        mousePosition = Vector(event.touches.get(0)?.clientX?.toDouble() ?: 0.0, event.touches.get(0)?.clientY?.toDouble() ?: 0.0) // cannot rely on mouse having already moved here
        mouseMovement = Vector(0.0, 0.0)
        onTouchStart(event, isOnUI)

        if(isOnUI) {
            SceneManager.currentScene.uiManager.getUI().onUIPressed(mousePosition, event)
        }
    }
    fun sealedOnTouchEnd(event: TouchEvent) {
        onTouchEnd(event, isOnUI)
    }

    open fun onMouseMove(event: MouseEvent, isOnUI: Boolean) {}
    open fun onMouseDown(event: MouseEvent, isOnUI: Boolean) {}
    open fun onMouseUp(event: MouseEvent, isOnUI: Boolean) {}
    open fun onMouseWheel(event: WheelEvent, isOnUI: Boolean) {}
    open fun onTouchMove(event: TouchEvent, isOnUI: Boolean) {}
    open fun onTouchStart(event: TouchEvent, isOnUI: Boolean) {}
    open fun onTouchEnd(event: TouchEvent, isOnUI: Boolean) {}
    open fun onKeyDown(event: KeyboardEvent) {}
    open fun onKeyUp(event: KeyboardEvent) {}
}