package input

import CardSimulatorClient
import framework.scene.SceneInput
import game.Table
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import util.math.Vector
import kotlin.math.pow

object Input : SceneInput() {

    private const val ZOOM_FACTOR = 1.05
    private const val MIN_ZOOM = 0.8
    private const val MAX_ZOOM = 9.0


    var isTableMoving = false
    var mousePositionTable = Vector(0.0, 0.0)

    override fun onMouseMove(event: MouseEvent, isOnUI: Boolean) {
        mousePositionTable = (mousePosition / Table.scale) - Table.offset

        if(isTableMoving) {
            Table.offset += mouseMovement / Table.scale
            CardSimulatorClient.requestRender()
        } else {
            Table.PlayerCursors.onCursorMovement(mousePositionTable)
        }

    }

    override fun onMouseDown(event: MouseEvent, isOnUI: Boolean) {
        if(isOnUI) {
            return
        }

        if(event.button.toInt() == 0) {
            isTableMoving = true
        }
    }

    override fun onMouseUp(event: MouseEvent, isOnUI: Boolean) {

        if(event.button.toInt() == 0) {
            isTableMoving = false
        }
    }

    override fun onMouseWheel(event: WheelEvent, isOnUI: Boolean) {
        if(isOnUI) {
            return
        }

        Table.scale /= ZOOM_FACTOR.pow(event.deltaY)

        Table.scale = minOf(MAX_ZOOM, maxOf(MIN_ZOOM, Table.scale))

        Table.offset = (mousePosition / Table.scale) - mousePositionTable
        CardSimulatorClient.requestRender()
    }

    override fun onKeyDown(event: KeyboardEvent) {
        super.onKeyDown(event)
    }

    override fun onKeyUp(event: KeyboardEvent) {
        super.onKeyUp(event)
    }
}