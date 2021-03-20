package framework.input

import framework.scene.SceneInput
import framework.scene.SceneManager
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent

/**
 * Input for all framework.scene
 */
object GenericInput : SceneInput() {
    const val KEY_ALT = 18
    const val KEY_A = 65
    const val KEY_D = 68
    const val KEY_F = 70
    const val KEY_G = 71
    const val KEY_R = 82
    const val KEY_S = 83
    const val KEY_PLUS = 171
    const val KEY_MINUS = 173

    private fun onMouseMove(event: Event) {
        if(event !is MouseEvent) throw RuntimeException("Event of wrong type")
        SceneManager.currentScene.input.sealedOnMouseMove(event)
    }

    private fun onMouseDown(event: Event) {
        if (event !is MouseEvent) throw RuntimeException("Event of wrong type")
        SceneManager.currentScene.input.sealedOnMouseDown(event)
    }

    private fun onMouseUp(event: Event) {
        if (event !is MouseEvent) throw RuntimeException("Event of wrong type")
        SceneManager.currentScene.input.sealedOnMouseUp(event)
    }

    private fun onMouseWheel(event: Event) {
        if (event !is WheelEvent) throw RuntimeException("Event of wrong type")
        SceneManager.currentScene.input.sealedOnMouseWheel(event)
    }

    private fun onKeyDown(event: Event) {
        if (event !is KeyboardEvent) throw RuntimeException("Event of wrong type")

        SceneManager.currentScene.input.onKeyDown(event)
    }

    private fun onKeyUp(event: Event) {
        if (event !is KeyboardEvent) throw RuntimeException("Event of wrong type")

        SceneManager.currentScene.input.onKeyUp(event)
    }

    private fun onTouchStart(event: Event) {
        if(event !is TouchEvent) throw RuntimeException("Event of wrong type")
        SceneManager.currentScene.input.sealedOnTouchStart(event)
    }

    private fun onTouchEnd(event: Event) {
        if (event !is TouchEvent) throw RuntimeException("Event of wrong type")
        SceneManager.currentScene.input.sealedOnTouchEnd(event)
    }

    private fun onTouchMove(event: Event) {
        if (event !is TouchEvent) throw RuntimeException("Event of wrong type")
        SceneManager.currentScene.input.sealedOnTouchMove(event)
    }

    fun init(canvas: HTMLCanvasElement) {
        with(canvas) {
            addEventListener("contextmenu", Event::preventDefault)
            addEventListener("mousemove", GenericInput::onMouseMove)
            addEventListener("mousedown", GenericInput::onMouseDown)
            addEventListener("mouseup", GenericInput::onMouseUp)
            addEventListener("wheel", GenericInput::onMouseWheel)
            addEventListener("touchstart", GenericInput::onTouchStart)
            addEventListener("touchend", GenericInput::onTouchEnd)
            addEventListener("touchmove", GenericInput::onTouchMove)
        }
        with(window) {
            addEventListener("keydown", GenericInput::onKeyDown)
            addEventListener("keyup", GenericInput::onKeyUp)
        }
    }
}