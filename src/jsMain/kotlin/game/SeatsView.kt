package game

import kotlinx.browser.document
import kotlinx.dom.clear
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import util.I18n

object SeatsView {
    val seatsListDiv = document.getElementById("seats-list") as HTMLDivElement

    fun init() {
        recreate()
    }

    fun recreate() {
        seatsListDiv.clear()

        Game.gameInfo.seats.forEach { seat ->
            seatsListDiv.append(createSeatEntry(seat.color,  seat.id, Game.playersBySeat[seat.id]))
        }
    }

    private fun createSeatEntry(color: String, id: Int, playerName: String?): HTMLDivElement {
        val seatDiv = (document.createElement("div") as HTMLDivElement).apply {
            style.display = "block"
        }
        val seatButton = (document.createElement("input") as HTMLInputElement).apply {
            type = "button"
            value = playerName ?: I18n.get("join-seat")
            style.backgroundColor = color
            className = "seat-button"
            name = id.toString()
        }
        seatDiv.appendChild(seatButton)

        seatButton.onclick = { Game.onJoinSeatRequest(id) }

        return seatDiv
    }
}