package input

import CardSimulatorClient
import ClientGameObjectReleasedMessage
import ClientUnstackGameObjectMessage
import framework.input.GenericInput.KEY_ALT
import framework.input.GenericInput.KEY_D
import framework.input.GenericInput.KEY_F
import framework.input.GenericInput.KEY_G
import framework.input.GenericInput.KEY_MINUS
import framework.input.GenericInput.KEY_PLUS
import framework.input.GenericInput.KEY_S
import framework.scene.SceneInput
import game.Game
import game.Table
import game.objects.GameObject
import game.objects.Stack
import kotlinx.browser.window
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.MouseEventInit
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.get
import util.Util
import util.math.Vector
import util.math.rectangleOf
import websocket.WebsocketClient
import kotlin.math.pow

object Input : SceneInput() {

    private const val ZOOM_FACTOR = 1.05
    private const val MIN_ZOOM = 0.8
    private const val MAX_ZOOM = 9.0

    private const val MAX_STACK_GRAB_DISTANCE = 10.0
    private const val STACK_GRAB_DELAY_MS = 500


    var mousePositionTable = Vector(0.0, 0.0)

    var isTableMoving = false
    var selectionAreaStart: Vector? = null



    var grabbedGameObject: GameObject? = null
    set(value) {
        field?.let { it.clientExtension.grabbed = false }
        field = value
        field?.let { it.clientExtension.grabbed = true }
    }
    var grabOffset: Vector = Vector()

    var grabStartPosition: Vector = Vector()
    var grabStartTime: Long = 0
    var stackGrabbed: Boolean = false

    var altDown: Boolean = false


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

        grabbedGameObject?.let {
            if (! checkStackGrab(it)) {
                return@let
            }
            val grabbedGameObject = grabbedGameObject ?: return@let // might have changed due to grab check

            grabbedGameObject.pos = mousePositionTable - grabOffset
            Table.selectedGameObjects.filter { it != grabbedGameObject }.forEach {
                it.pos += mouseMovement / Table.scale
            }

            Table.selectedGameObjects.forEach { Table.onGameObjectMoved(it) }
        }
    }

    private fun checkStackGrab(grabbedGameObject: GameObject): Boolean {
        if (grabbedGameObject is Stack && !stackGrabbed) {
            val passedTime = Util.currentTimeMillis() - grabStartTime
            if ((grabStartPosition - mousePositionTable).length() > MAX_STACK_GRAB_DISTANCE
                && passedTime < STACK_GRAB_DELAY_MS
                && Table.selectedGameObjects.none { it != grabbedGameObject}) {
                grabbedGameObject.topObject?.let {
                    Table.selectedGameObjects.clear() // remove stack if currently selected
                    Table.selectedGameObjects.add(it)
                    Input.grabbedGameObject = it
                    it.pos = mousePositionTable - grabOffset
                    WebsocketClient.send(ClientUnstackGameObjectMessage(it.id))
                }
                return false
            } else if (passedTime >= STACK_GRAB_DELAY_MS) {
                Table.selectedGameObjects.add(grabbedGameObject)
                stackGrabbed = true
            } else {
                return false
            }
        }
        return true
    }


    override fun onMouseDown(event: MouseEvent, isOnUI: Boolean) {
        if(isOnUI) {
            return
        }

        if(event.button.toInt() == 0) {
            if(event.shiftKey) {
                selectionAreaStart = mousePositionTable
            } else {
                val pressedGameObject = Table.renderedGameObjects.findLast { mousePositionTable in it.rect } // sorted by last moved --> search last one
                if(pressedGameObject != null) {
                    if(!Table.selectedGameObjects.contains(pressedGameObject)) {

                        if(! event.ctrlKey) {
                            Table.selectedGameObjects.clear()
                        }

                        if(pressedGameObject !is Stack) {
                            Table.selectedGameObjects.add(pressedGameObject)
                        } else {
                            Table.selectedGameObjects.remove(pressedGameObject)
                        }
                    }

                    // grab
                    if(Game.ownSeat != null) {
                        grabbedGameObject = pressedGameObject
                        grabOffset = mousePositionTable - pressedGameObject.pos

                        grabStartPosition = mousePositionTable
                        grabStartTime = Util.currentTimeMillis()

                        if(pressedGameObject is Stack) {
                            stackGrabbed = false
                            window.setTimeout({
                                if(grabbedGameObject == pressedGameObject && !stackGrabbed) {
                                    checkStackGrab(pressedGameObject)
                                }
                            }, STACK_GRAB_DELAY_MS)
                        }

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
            if(grabbedGameObject is Stack && !stackGrabbed) {
                with(Table.selectedGameObjects) {
                    clear()
                    add(grabbedGameObject!!)
                }
            } else {
                grabbedGameObject?.let {
                    Table.onGameObjectMoved(it)
                    WebsocketClient.send(ClientGameObjectReleasedMessage(it.pos, it.id))
                }
            }

            selectionAreaStart = null
            grabbedGameObject = null
            isTableMoving = false

            Table.gameObjects.forEach {
                it.clientExtension.grabbed = false
                it.clientExtension.lastGrabTime = Util.currentTimeMillis()
            }
        }
    }

    override fun onMouseWheel(event: WheelEvent, isOnUI: Boolean) {
        if(isOnUI) {
            return
        }

        var amount = event.deltaY
        if(event.deltaMode == 0) amount /= 33  // 0: pixel, 1: lines, 2: pages

        Table.scale /= ZOOM_FACTOR.pow(amount)

        Table.scale = minOf(MAX_ZOOM, maxOf(MIN_ZOOM, Table.scale))

        Table.offset = (mousePosition / Table.scale) - mousePositionTable
    }

    override fun onTouchMove(event: TouchEvent, isOnUI: Boolean) {
        event.preventDefault()

        val mouseEvent = MouseEvent("mousemove", MouseEventInit(
            clientX = event.touches[0]?.clientX ?: 0,
            clientY = event.touches[0]?.clientY ?: 0
        ))
        console.log("TouchMove - pos: $mousePosition, mov: $mouseMovement")
        onMouseMove(mouseEvent, isOnUI)
    }

    override fun onTouchStart(event: TouchEvent, isOnUI: Boolean) {
        event.preventDefault()

        if(isOnUI) {
            return
        }

        val mouseEvent = MouseEvent("mousedown", MouseEventInit(
            clientX = event.touches[0]?.clientX ?: 0,
            clientY = event.touches[0]?.clientY ?: 0
        ))
        onMouseDown(mouseEvent, isOnUI)
    }

    override fun onTouchEnd(event: TouchEvent, isOnUI: Boolean) {
        event.preventDefault()

        if(isOnUI) {
            return
        }

        val mouseEvent = MouseEvent("mouseup", MouseEventInit(
            clientX = event.touches[0]?.clientX ?: 0,
            clientY = event.touches[0]?.clientY ?: 0
        ))
        onMouseUp(mouseEvent, isOnUI)
    }

    override fun onKeyDown(event: KeyboardEvent) {
        if (event.keyCode == KEY_F) {
            Game.onFlipRequested()
        }

        if (event.keyCode == KEY_G) {
            Game.onGroupRequested()
        }

        if (event.keyCode == KEY_S) {
            Game.onShuffleRequested()
        }

        if (event.keyCode == KEY_D) {
            Game.onDealRequested()
        }

        if (event.keyCode == KEY_PLUS) {
            Table.scale *= ZOOM_FACTOR
        }
        if (event.keyCode == KEY_MINUS) {
            Table.scale /= ZOOM_FACTOR
        }
        if(event.keyCode == KEY_ALT) {
            altDown = true
            event.preventDefault()
        }
    }

    override fun onKeyUp(event: KeyboardEvent) {

        if(event.keyCode == KEY_ALT) {
            altDown = false
            event.preventDefault()
        }
    }

    fun getObjectUnderMouse() = Table.gameObjects.find { mousePositionTable in it.rect }
}