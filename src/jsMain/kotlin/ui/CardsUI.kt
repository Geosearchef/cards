import framework.scene.UIManager
import framework.ui.SceneUI
import game.Game
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import util.I18n
import util.math.Rectangle
import util.math.Vector

class CardsUI(width: Int, height: Int) : SceneUI(width, height) {

    companion object {
        const val CLOSE_OVERLAY_DIST = 20.0
        const val EXTENDED_OVERLAY_DIST = 300.0

        fun getUI() = CardSimulatorClient.DefaultScene.uiManager.getUI() as CardsUI
    }

    val playerNoteTextfield = document.getElementById("player-note-textfield") as HTMLInputElement

    var autoHideEnabled: Boolean = false
    val hideButton = document.getElementById("hide-button") as HTMLInputElement

    val overlays = listOf(
        UIOverlay(document.getElementById("actions-overlay") as HTMLDivElement,
            closeArea = Rectangle(0.0, 0.0, CLOSE_OVERLAY_DIST, height / 2.0),
            extendedArea = Rectangle(0.0, 0.0, EXTENDED_OVERLAY_DIST, height * 0.75)
        ),
        UIOverlay(document.getElementById("seats-overlay") as HTMLDivElement,
            closeArea = Rectangle(width - CLOSE_OVERLAY_DIST, 0.0, CLOSE_OVERLAY_DIST, height / 2.0),
            extendedArea = Rectangle(width - EXTENDED_OVERLAY_DIST, 0.0, EXTENDED_OVERLAY_DIST, height * 0.75)
        )
    )

    fun onMouseMove(mousePosition: Vector) {
        if(!autoHideEnabled) {
            return
        }

        overlays.forEach { overlay ->
            if(!overlay.currentlyShown && mousePosition in overlay.closeArea) {
                overlay.currentlyShown = true
            } else if(overlay.currentlyShown && mousePosition !in overlay.extendedArea) {
                overlay.currentlyShown = false
            }

            overlay.element.style.display = if(overlay.currentlyShown) "block" else "none"
        }
    }

    fun onHideButtonPressed() {
        autoHideEnabled = !autoHideEnabled

        if(!autoHideEnabled) {
            overlays.forEach { it.element.style.display = "block" }
        }

        hideButton.value = if(autoHideEnabled) I18n.get("show-ui") else I18n.get("hide-ui")
    }

    data class UIOverlay(val element: HTMLDivElement, val closeArea: Rectangle, val extendedArea: Rectangle, var currentlyShown: Boolean = false)

    init {
        playerNoteTextfield.oninput = {
            Game.onPlayerNoteChanged(playerNoteTextfield.value)
        }
    }
    fun setPlayerNote(s: String) {
        playerNoteTextfield.value = s
    }

    override fun isMouseEventOnUI(mousePosition: Vector): Boolean {
        return false
    }
}

/**
 * can be integrated into framework.scene "manager"
 */
object CardsUIManager : UIManager {

    var uiInstance = CardsUI(300, 200)

    override fun regenerateUI(width: Int, height: Int) {
        uiInstance = CardsUI(width, height)
    }

    override fun getUI(): SceneUI = uiInstance
}