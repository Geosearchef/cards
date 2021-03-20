package game

import CardSimulatorClient
import CardsUI
import ClientAdminDeleteAllGameObjectsMessage
import ClientAdminDeleteGameObjectsMessage
import ClientAdminSpawnDeckMessage
import ClientDealStackMessage
import ClientFlipObjectMessage
import ClientGroupObjectsMessage
import ClientJoinSeatMessage
import ClientShuffleStacksMessage
import ClientSortPlayerZoneMessage
import GameInfo
import Message
import SeatInfo
import ServerAddGameObjectMessage
import ServerCursorPositionMessage
import ServerGameObjectPositionMessage
import ServerLoginMessage
import ServerPlayerJoinSeatMessage
import ServerPlayerLeaveSeatMessage
import ServerRemoveGameObjectMessage
import ServerSetGameObjectsFlippedMessage
import ServerStackInfoMessage
import assets.AssetManager
import game.objects.Stack
import game.objects.StackableGameObject
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLBodyElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import util.I18n
import util.Util
import websocket.WebsocketClient

object Game {

    var loggedIn = false
    lateinit var gameInfo: GameInfo
    var serverTimestampOffset: Long = 0L // add to server timestamps to get local
    var admin: Boolean = false

    val playersBySeat: MutableMap<Int, String> = HashMap()
    val players: Collection<String> get() = playersBySeat.values

    var ownSeat: Int? = null

    fun onServerMessage(msg: Message) {
        when(msg) {
            is ServerLoginMessage -> {
                console.log("Successfully logged in, got game info, table with ${msg.gameInfo.seats.size}")

                gameInfo = msg.gameInfo
                AssetManager.ASSET_TOKEN = msg.assetToken
                serverTimestampOffset = Util.currentTimeMillis() - msg.serverTimestamp
                console.log("Timstamp offset: $serverTimestampOffset ms ahead")

                admin = msg.admin
                if(admin) {
                    console.log("We are admin!")
                    (document.getElementById("admin-actions-list") as HTMLDivElement).style.display = "block" // TODO: extract to UI
                }

                SeatsView.init()
                loggedIn = true
            }

            is ServerPlayerJoinSeatMessage -> {
                playersBySeat[msg.seatId] = msg.playerName

                if(msg.playerName == CardSimulatorClient.username) {
                    ownSeat = msg.seatId
                }

                SeatsView.recreate()
            }

            is ServerPlayerLeaveSeatMessage -> {
                playersBySeat.remove(msg.seatId)
                SeatsView.recreate()
            }

            is ServerCursorPositionMessage -> {
                Table.PlayerCursors.onServerCursorPositionUpdate(msg.player, msg.pos)
            }

            is ServerAddGameObjectMessage -> {
                msg.gameObject.lastTouchedOnServer += serverTimestampOffset
                Table.gameObjects.add(msg.gameObject)
                console.log("Got game object: ")
                console.log(msg.gameObject)
            }

            is ServerRemoveGameObjectMessage -> {
//                val gameObject = Table.gameObjects.find { it.id == msg.id }
//                if(gameObject is Stack) {
//                    gameObject.stackedObjects.forEach { it.stack = null } // TODO: does this cause inconsistency?
//                }

                Table.gameObjects.removeAll { it.id == msg.id }
                Table.selectedGameObjects.removeAll { it.id == msg.id }

                console.log("Removed game object $msg.id")
            }

            is ServerGameObjectPositionMessage -> {
//                console.log("Got position ${msg.pos} for ${msg.id}")
                Table.gameObjects.find { it.id == msg.id }?.let { Table.onServerGameObjectPosition(it, msg.pos) }
            }

            is ServerSetGameObjectsFlippedMessage -> {
                msg.objsStatus.forEach { e ->
                    Table.gameObjects.find { it.id == e.key }?.let {
                        it.lastTouchedOnServer = Util.currentTimeMillis()
                        it.flipped = e.value
                    }
                }
            }

            is ServerStackInfoMessage -> run {
                val stack = Table.gameObjects.filterIsInstance<Stack>().find { it.id == msg.id }
                if(stack == null) {
                    console.error("Couldn't find a stack to update for id: ${msg.id}")
                    return@run
                }

                console.log("Got stack ${msg.id} update: ${msg.stackedObjects}")

//                val stackedObjects = Table.gameObjects
//                    .filterIsInstance<StackableGameObject>()
//                    .filter { msg.stackedObjects.contains(it.id) }

                val stackedObjects = msg.stackedObjects.map { id -> Table.gameObjects.find { it.id == id } }.filterNotNull().filterIsInstance<StackableGameObject>()

                if(stackedObjects.size != msg.stackedObjects.size) {
                    console.error("Couldn't find all stackables in stack, found: ${stackedObjects.map { it.id }}, wanted: ${msg.stackedObjects}")
                    return@run
                }

                Table.onServerStackInfo(stack, stackedObjects)
            }

            else -> {
                console.log("Received message of unknown type: ${msg::class}")
            }
        }
    }

    fun onJoinSeatRequest(seatId: Int) {
        WebsocketClient.send(ClientJoinSeatMessage(seatId))
    }

    fun onDealRequested() {
        if (Table.selectedGameObjects.isNotEmpty()) {
            Table.selectedGameObjects.find { (it as? StackableGameObject)?.stack == null }?.let { stack ->
                WebsocketClient.send(ClientDealStackMessage(stack.id))
            }
        }
    }

    fun onShuffleRequested() {
        val selectedStacks = Table.selectedGameObjects.filterIsInstance<Stack>()
        if (selectedStacks.isNotEmpty()) {
            WebsocketClient.send(ClientShuffleStacksMessage(Table.selectedGameObjects.map { it.id }.toTypedArray()))
        }
    }

    fun onGroupRequested() {
        if (Table.selectedGameObjects.isNotEmpty()) {
            WebsocketClient.send(ClientGroupObjectsMessage(Table.selectedGameObjects.map { it.id }.toTypedArray()))
        }
    }

    fun onFlipRequested() {
        if (Table.selectedGameObjects.isNotEmpty()) {
            WebsocketClient.send(ClientFlipObjectMessage(Table.selectedGameObjects
                .filter { (it as? StackableGameObject)?.stack == null }.map { it.id }.toTypedArray()
            ))
        }
    }

    fun onSortRequested() {
        WebsocketClient.send(ClientSortPlayerZoneMessage())
    }

    fun onSpawnDeckRequested(deck: String) {
        WebsocketClient.send(ClientAdminSpawnDeckMessage(deck))
    }

    fun onGameObjectsDeleteRequested() {
        if(Table.selectedGameObjects.isNotEmpty()) {
            WebsocketClient.send(ClientAdminDeleteGameObjectsMessage(Table.selectedGameObjects.map{ it.id }))
        }
    }

    fun onAllGameObjectsDeleteRequested() {
        WebsocketClient.send(ClientAdminDeleteAllGameObjectsMessage())
    }

    fun getPlayerColor(playerName: String) : String? = getPlayerSeat(playerName)?.color

    fun getPlayerSeat(playerName: String): SeatInfo? {
        return playersBySeat.entries.find { it.value == playerName }?.let { entry ->
            gameInfo.seats.find { it.id == entry.key }
        }
    }

    //TODO: extract to UI
    var fullscreen = false

    fun init() {
        val onloadFunction = {
            (document.getElementById("flip-button") as HTMLInputElement).onclick = { Game.onFlipRequested() }
            (document.getElementById("group-button") as HTMLInputElement).onclick = { Game.onGroupRequested() }
            (document.getElementById("shuffle-button") as HTMLInputElement).onclick = { Game.onShuffleRequested() }
            (document.getElementById("deal-button") as HTMLInputElement).onclick = { Game.onDealRequested() }
            (document.getElementById("sort-button") as HTMLInputElement).onclick = { Game.onSortRequested() }
            (document.getElementById("fullscreen-button") as HTMLInputElement).onclick = {
                if(fullscreen) {
                    console.log("Exiting fullscreen")
                    document.exitFullscreen()
                    fullscreen = false
                } else {
                    console.log("Entering fullscreen")
                    val body = (document.getElementById("body") as HTMLBodyElement)
                    body.requestFullscreen()
                    fullscreen = true
                }
            }
            (document.getElementById("hide-button") as HTMLInputElement).onclick = { (CardSimulatorClient.DefaultScene.uiManager.getUI() as CardsUI).onHideButtonPressed() }
            (document.getElementById("spawn-button") as HTMLInputElement).onclick = {
                val deck = window.prompt("${I18n.get("spawn-deck-confirm")} (${gameInfo.availableDecks.joinToString(", ")})")
                if(deck == null) {
                    window.alert("$deck ${I18n.get("invalid-deck")}")
                } else {
                    onSpawnDeckRequested(deck)
                }
            }
            (document.getElementById("delete-button") as HTMLInputElement).onclick = {
                if(window.confirm(message = "${I18n.get("delete-obj-confirm")} ${Table.selectedGameObjects.map { it.id }} ?")) {
                    Game.onGameObjectsDeleteRequested()
                }
            }
            (document.getElementById("deleteall-button") as HTMLInputElement).onclick = {
                if(window.confirm(message = I18n.get("delete-all-confirm"))) {
                    Game.onAllGameObjectsDeleteRequested()
                }
            }

            "".asDynamic()
        }

        js("addOnLoadCallback(onloadFunction)") // don't hook onload directly as i18n also uses it
    }

}