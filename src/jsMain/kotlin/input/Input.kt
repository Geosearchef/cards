package input

import CardSimulatorClient
import framework.scene.SceneInput
import game.Game
import game.GameObject
import game.Table
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import util.math.Vector
import util.math.rectangleOf
import kotlin.math.pow

object Input : SceneInput() {

    private const val ZOOM_FACTOR = 1.05
    private const val MIN_ZOOM = 0.8
    private const val MAX_ZOOM = 9.0


    var mousePositionTable = Vector(0.0, 0.0)

    var isTableMoving = false
    var selectionAreaStart: Vector? = null

    var grabbedGameObject: GameObject? = null
    var grabOffset: Vector = Vector()

    override fun onMouseMove(event: MouseEvent, isOnUI: Boolean) {
        mousePositionTable = (mousePosition / Table.scale) - Table.offset

        if(isTableMoving) {
            Table.offset += mouseMovement / Table.scale
            CardSimulatorClient.requestRender()
        } else {
            Table.PlayerCursors.onCursorMovement(mousePositionTable)
        }

        selectionAreaStart?.let { selectionAreaStart ->
            Table.setSelection(rectangleOf(selectionAreaStart, mousePositionTable))
        }

        grabbedGameObject?.let { grabbedGameObject ->
            grabbedGameObject.pos = mousePositionTable - grabOffset
            Table.selectedGameObjects.filter { it != grabbedGameObject }.forEach {
                it.pos += mouseMovement / Table.scale
            }

            Table.selectedGameObjects.forEach { Table.onGameObjectMoved(it) }
        }
    }

    override fun onMouseDown(event: MouseEvent, isOnUI: Boolean) {
        if(isOnUI) {
            return
        }

        if(event.button.toInt() == 0) {
            if(event.shiftKey) {
                selectionAreaStart = mousePositionTable
            } else {
                val pressedGameObject = Table.gameObjects.findLast { mousePositionTable in it.rect } // sorted by last moved --> search last one
                if(pressedGameObject != null) {
                    if(!Table.selectedGameObjects.contains(pressedGameObject)) {
                        Table.selectedGameObjects.clear()
                        Table.selectedGameObjects.add(pressedGameObject)
                    }

                    // grab
                    if(Game.ownSeat != null) {
                        grabbedGameObject = pressedGameObject
                        grabOffset = mousePositionTable - pressedGameObject.pos

                        Table.selectedGameObjects.forEach { it.clientExtension.grabbed = true }
                    }
                } else {
                    isTableMoving = true
                    Table.selectedGameObjects.clear()
                }
            }
        }
    }

    override fun onMouseUp(event: MouseEvent, isOnUI: Boolean) {

        if(event.button.toInt() == 0) {
            selectionAreaStart = null
            grabbedGameObject = null
            isTableMoving = false

            Table.gameObjects.forEach { it.clientExtension.grabbed = false }
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