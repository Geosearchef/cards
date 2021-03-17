package rendering

import CardSimulatorClient
import assets.AssetManager
import framework.rendering.*
import framework.scene.Scene.SceneRenderer
import game.Game
import game.Table
import game.objects.Card
import game.objects.Stack
import input.Input
import org.w3c.dom.*
import util.math.Rectangle
import util.math.Vector

object Rendering : SceneRenderer {

    private const val PLAYER_CURSOR_RADIUS = 5.0
    private const val CARD_OUTLINE_COLOR = "#222222"
    private const val CARD_OUTLINE_WIDTH = 1.2
    private const val CARD_OUTLINE_RADIUS = 4.0
    private const val AREA_SELECTION_COLOR = "#4fb3ff"
    private const val CARD_OUTLINE_COLOR_SELECTED = "#4fb3ff"
    private const val STACK_COUNT_OUTLINE_RADIUS = 4.0
    private val STACK_COUNT_SIZE = Vector(20.0, 20.0)

    override fun render(ctx: CanvasRenderingContext2D) {
        if(Game.loggedIn) {
            renderTable(ctx)
        }
    }

    fun renderTable(ctx: CanvasRenderingContext2D) {
        ctx.scale(Table.scale, Table.scale)
        ctx.translate(Table.offset.x, Table.offset.y)

//        for(x in 0 until 10) {
//            for(y in 0 until 10) {
//                ctx.
//            }
//        }

//        for (var x = (center.screen().x - canvas.width / 2 / scaleFactor) - ((center.screen().x - canvas.width / 2 / scaleFactor) % CELL_SCALE) + CELL_SCALE;x < center.screen().x + canvas.width / 2 / scaleFactor;x += CELL_SCALE) {
//            ctx.moveTo(x + 0.5, -10000/*center.screen().y - canvas.height / 2*/);
//            ctx.lineTo(x + 0.5, 10000/*center.screen().y + canvas.height / 2 / scaleFactor / scaleFactor*/);
//        }
//        for (var y = (center.screen().y - canvas.height / 2 / scaleFactor) - ((center.screen().y - canvas.height / 2 / scaleFactor) % CELL_SCALE) + CELL_SCALE;y < center.screen().y + canvas.height / 2 / scaleFactor;y += CELL_SCALE) {
//            ctx.moveTo(-10000, y + 0.5);
//            ctx.lineTo(10000, y + 0.5);
//        }

        renderPlayerZones(ctx)
        renderGameObjects(ctx)
        renderPlayerCursors(ctx)
        renderSelectionArea(ctx)

        ctx.setIdentityMatrix()
    }

    private fun renderPlayerCursors(ctx: CanvasRenderingContext2D) {
        Game.players.filter { it != CardSimulatorClient.username }.forEach { player ->
            Table.PlayerCursors.renderedCursorPositionByPlayer[player]?.let { cursor ->
                ctx.color(Game.getPlayerColor(player) ?: "444444")
                ctx.fillCircle(cursor, PLAYER_CURSOR_RADIUS)
            }
        }
    }

    private fun renderGameObjects(ctx: CanvasRenderingContext2D) {
        ctx.imageSmoothingEnabled = true
        ctx.imageSmoothingQuality = ImageSmoothingQuality.HIGH

        Table.gameObjects.sortBy { gameObject ->
            if (Game.gameInfo.playerZones.any { gameObject in it }) { // TODO: this is pretty performance hungry
                return@sortBy gameObject.rect.x
            } else {
                return@sortBy gameObject.lastTouchedOnServer.toDouble()
            }

        } // recently moved cards are at the top

        Table.renderedGameObjects.forEach { gameObject ->
            val inOtherPlayerZone = Game.gameInfo.playerZones.filterIndexed { index, playerZone -> index != Game.ownSeat && gameObject in playerZone }.any()

            when(gameObject) {
                is Card -> {
                    renderCard(ctx, gameObject.rect, gameObject.getUsedAsset(inOtherPlayerZone), Table.selectedGameObjects.contains(gameObject))
                }

                is Stack -> {
                    renderStack(ctx, gameObject.rect, gameObject.getUsedAsset(inOtherPlayerZone), Table.selectedGameObjects.contains(gameObject), gameObject.stackedObjects.size)
                }

                else -> console.log("Couldn't render GameObject of unknown type")
            }
        }
    }

    private fun renderStack(ctx: CanvasRenderingContext2D, rect: Rectangle, asset: String?, selected: Boolean, stackedObjectsCount: Int) {
        renderCard(ctx, rect, asset, selected)

        // render count
        ctx.color("#FFFFFF")
        ctx.globalAlpha = 0.9
        ctx.roundRect(
            Rectangle(rect.center - STACK_COUNT_SIZE / 2.0, STACK_COUNT_SIZE.x, STACK_COUNT_SIZE.y),
            STACK_COUNT_OUTLINE_RADIUS
        )
        ctx.fill()
        ctx.globalAlpha = 1.0

        ctx.color("#000000")
        ctx.lineWidth = 0.5
        ctx.roundRect(
            Rectangle(rect.center - STACK_COUNT_SIZE / 2.0, STACK_COUNT_SIZE.x, STACK_COUNT_SIZE.y),
            STACK_COUNT_OUTLINE_RADIUS
        )
        ctx.lineWidth = 1.0
        ctx.stroke()

        ctx.color("#000000")
        ctx.font = "14px sans-serif"
        ctx.textBaseline = CanvasTextBaseline.MIDDLE;
        ctx.fillTextCentered(stackedObjectsCount.toString(), rect.center + Vector(y = 0.25))
        ctx.textBaseline = CanvasTextBaseline.BOTTOM;
    }

    private fun renderCard(ctx: CanvasRenderingContext2D, rect: Rectangle, asset: String?, selected: Boolean) {
        if (asset != null) {
            // white background
            ctx.color("#FFFFFF")
            ctx.roundRect(rect, CARD_OUTLINE_RADIUS)
            ctx.fill()

            AssetManager.get(asset)?.wrappedImage?.let { image ->
                ctx.drawImage(image, rect.pos.x, rect.pos.y, rect.width, rect.height)
            }

            ctx.color(if (selected) CARD_OUTLINE_COLOR_SELECTED else CARD_OUTLINE_COLOR)
            ctx.lineWidth = CARD_OUTLINE_WIDTH
            ctx.roundRect(rect, CARD_OUTLINE_RADIUS)
            ctx.stroke()
            ctx.lineWidth = 1.0
        } else {
            ctx.color("#333333")
            ctx.fillRect(rect)
        }
    }

    private fun renderPlayerZones(ctx: CanvasRenderingContext2D) {
        Game.gameInfo.playerZones.forEach { playerZone ->
            ctx.globalAlpha = 0.4
            val playerColor = Game.gameInfo.seats[playerZone.seatId].color
            ctx.color(playerColor)
            ctx.fillRect(playerZone.rect)
            ctx.globalAlpha = 1.0

            Game.playersBySeat[playerZone.seatId]?.let { playerName ->
                ctx.font = "18px sans-serif"
//                ctx.lineWidth = 2.0
//                ctx.miterLimit= 2.0
////                ctx.color("#FFFFFF")
//                ctx.color("#FFFFFF")
//                ctx.strokeTextCentered(playerName, Vector(playerZone.rect.center.x, playerZone.rect.y) + Vector(0.0, 10.0))
                ctx.color("#000000")
//                ctx.color(playerColor)
                //TODO: alternative: playerColor with black border, LARGE
                if(playerZone.rect.y < 0) {
                    ctx.fillTextCentered(playerName, Vector(playerZone.rect.center.x, playerZone.rect.y) - Vector(y = 16.0))
                } else {
                    ctx.fillTextCentered(playerName, Vector(playerZone.rect.center.x, playerZone.rect.y + playerZone.rect.height) + Vector(y = 16.0))
                }
            }
        }
    }

    private fun renderSelectionArea(ctx: CanvasRenderingContext2D) {
        Input.selectionAreaStart?.let{ selectionAreaStart ->
            ctx.color(AREA_SELECTION_COLOR)
            ctx.globalAlpha = 0.3
            ctx.fillRect(selectionAreaStart, Input.mousePositionTable)
            ctx.globalAlpha = 1.0
        }

    }
}