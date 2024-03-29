package rendering

import CardSimulatorClient
import CardSimulatorOptions
import assets.AssetManager
import framework.rendering.*
import framework.scene.Scene.SceneRenderer
import game.Game
import game.Table
import game.objects.*
import input.Input
import org.w3c.dom.*
import util.I18n
import util.Util
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
    private val SHUFFLED_INDICATOR_SIZE = Vector(80.0, 20.0)
    private const val SHUFFLED_INDICATOR_DURATION_MS = 2000

    private const val PLAYER_ZONE_CORNER_RADIUS = 10.0
    private const val CARD_PEEK_SCALE = 2.5

    var screenRectOnTable = Rectangle(0.0,0.0,10.0,10.0)
    var width = 0
    var height = 0

    override fun render(ctx: CanvasRenderingContext2D, width: Int, height: Int) {
        Rendering.width = width
        Rendering.height = height

        if(Game.loggedIn) {
            renderTable(ctx)
        }
    }

    fun renderTable(ctx: CanvasRenderingContext2D) {
        ctx.scale(Table.scale, Table.scale)
        ctx.translate(Table.offset.x, Table.offset.y)

        screenRectOnTable = Rectangle(- Table.offset, (width) / Table.scale, height / Table.scale)

//        ctx.color("#222222")
//        ctx.fillRect(screenRectOnTable)

        ctx.color("#333333")
//        ctx.fillRect(screenRectOnTable)
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

        renderCardPeek(ctx)

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
            val inOtherPlayerZone = inOtherPlayerZone(gameObject)

            if(gameObject.rect.corners.none { it in screenRectOnTable }) {
                return@forEach
            }

            when(gameObject) {
                is Card -> {
                    renderCard(
                        ctx,
                        gameObject.rect,
                        gameObject.getUsedAsset(inOtherPlayerZone),
                        Table.selectedGameObjects.contains(gameObject)
                    )
                }

                is Stack -> {
                    renderStack(
                        ctx,
                        gameObject.rect,
                        gameObject.getUsedAsset(inOtherPlayerZone),
                        Table.selectedGameObjects.contains(gameObject),
                        gameObject.stackedObjects.size,
                        showShuffledIndicator = (Util.currentTimeMillis() - gameObject.clientExtension.lastShuffled <= SHUFFLED_INDICATOR_DURATION_MS)
                    )
                }

                is TokenObject -> {
                    renderToken(
                        ctx,
                        gameObject.rect,
                        gameObject.color,
                        Table.selectedGameObjects.contains(gameObject),
                    )
                }

                is NonStackableGameObject -> {
                    renderNonStackable(
                        ctx,
                        gameObject.rect,
                        gameObject.getUsedAsset(inOtherPlayerZone),
                        Table.selectedGameObjects.contains(gameObject)
                    )
                }

                else -> console.log("Couldn't render GameObject of unknown type")
            }
        }
    }

    private fun inOtherPlayerZone(gameObject: GameObject) =
        Game.gameInfo.playerZones.filterIndexed { index, playerZone -> index != Game.ownSeat && gameObject in playerZone }
            .any()

    private fun renderStack(ctx: CanvasRenderingContext2D, rect: Rectangle, asset: String?, selected: Boolean, stackedObjectsCount: Int, showShuffledIndicator: Boolean) {
        renderCard(ctx, rect, asset, selected)

        // render count
        renderRoundedTextOverlayBox(
            ctx,
            Rectangle(rect.center - STACK_COUNT_SIZE / 2.0, STACK_COUNT_SIZE.x, STACK_COUNT_SIZE.y),
            STACK_COUNT_OUTLINE_RADIUS,
            stackedObjectsCount.toString())

        // render shuffle indicator
        if(showShuffledIndicator) {
            renderRoundedTextOverlayBox(
                ctx,
                Rectangle(rect.center - SHUFFLED_INDICATOR_SIZE / 2.0, SHUFFLED_INDICATOR_SIZE.x, SHUFFLED_INDICATOR_SIZE.y),
                STACK_COUNT_OUTLINE_RADIUS,
                I18n.get("shuffled-indicator")
            )
        }
    }

    private fun renderRoundedTextOverlayBox(ctx: CanvasRenderingContext2D, rect: Rectangle, outlineRadius: Double, text: String) {
        ctx.color("#FFFFFF")
        ctx.globalAlpha = 0.9
        ctx.roundRect(rect, outlineRadius)
        ctx.fill()
        ctx.globalAlpha = 1.0

        ctx.color("#000000")
        ctx.lineWidth = 0.5
        ctx.roundRect(rect, outlineRadius)
        ctx.lineWidth = 1.0
        ctx.stroke()

        ctx.color("#000000")
        ctx.font = "14px sans-serif"
        ctx.fillTextCentered(text, rect.center + Vector(y = 0.25))
    }

    private fun renderNonStackable(ctx: CanvasRenderingContext2D, rect: Rectangle, asset: String?, selected: Boolean, peek: Boolean = false) {
        renderCard(ctx, rect, asset, selected, peek, outline = false)
    }

    private fun renderCard(ctx: CanvasRenderingContext2D, rect: Rectangle, asset: String?, selected: Boolean, peek: Boolean = false, outline: Boolean = true) {
        if (asset != null) {
            // white background
            if(outline) {
                ctx.color("#FFFFFF")
                ctx.roundRect(rect, CARD_OUTLINE_RADIUS)
                ctx.fill()
            }

            AssetManager.get(asset)?.let { it ->
                val image = if(CardSimulatorOptions.MIPMAPPING) it.getMipmap(rect.width.toInt() * 2, rect.height.toInt() * 2) else it.wrappedImage
                ctx.drawImage(image, rect.pos.x, rect.pos.y, rect.width, rect.height)
            }

            if(selected || outline) {
                ctx.color(if (selected) CARD_OUTLINE_COLOR_SELECTED else CARD_OUTLINE_COLOR)
                ctx.lineWidth = CARD_OUTLINE_WIDTH * (if(peek) CARD_PEEK_SCALE else 1.0)
                ctx.roundRect(rect, CARD_OUTLINE_RADIUS * (if(peek) CARD_PEEK_SCALE else 1.0))
                ctx.stroke()
                ctx.lineWidth = 1.0
            }
        } else {
            ctx.color("#333333")
            ctx.fillRect(rect)
        }
    }

    private fun renderToken(ctx: CanvasRenderingContext2D, rect: Rectangle, color: String, selected: Boolean, peek: Boolean = false) {
        ctx.color(color)
        ctx.fillCircle(rect.center, rect.width)

        ctx.color(if (selected) CARD_OUTLINE_COLOR_SELECTED else CARD_OUTLINE_COLOR)
        ctx.lineWidth = CARD_OUTLINE_WIDTH * (if(peek) CARD_PEEK_SCALE else 1.0)
        ctx.strokeEllipse(rect.center, rect.width, rect.height)
        ctx.lineWidth = 1.0
    }

    private fun renderPlayerZones(ctx: CanvasRenderingContext2D) {
        Game.gameInfo.playerZones.forEach { playerZone ->
            val shouldBeHidden = !Game.playersBySeat.entries.isEmpty() && Game.playersBySeat[playerZone.seatId] == null
            if(shouldBeHidden) {
                return@forEach
            }

//            ctx.globalAlpha = 0.4
            ctx.lineWidth = 2.0
            val playerColor =
                if(shouldBeHidden)
                    "#CCCCCC"
                else
                    Game.gameInfo.seats[playerZone.seatId].color
            ctx.color(playerColor)
            ctx.globalAlpha = 0.2
            ctx.roundRect(playerZone.rect, PLAYER_ZONE_CORNER_RADIUS)
            ctx.fill()
            ctx.globalAlpha = 1.0
            ctx.roundRect(playerZone.rect, PLAYER_ZONE_CORNER_RADIUS)
            ctx.stroke()

            Game.playersBySeat[playerZone.seatId]?.let { playerName ->
                ctx.font = "16px sans-serif"
                ctx.color("#000000")
//                ctx.color(playerColor)
                var yPos = 0.0
                if(playerZone.rect.y < 0) {
                    yPos = playerZone.rect.y + playerZone.rect.height - 15.0
                } else {
                    yPos = playerZone.rect.y + 15.0
                }

                ctx.fillTextLeft(playerName, Vector(playerZone.rect.x + 7.0, yPos), textBaseline = CanvasTextBaseline.MIDDLE)
            }

            Game.playerNotesBySeat[playerZone.seatId]?.let { note ->
                ctx.font = "16px sans-serif"
                ctx.color("#000000")

                var yPos = 0.0
                if(playerZone.rect.y < 0) {
                    yPos = playerZone.rect.y + playerZone.rect.height - 15.0
                } else {
                    yPos = playerZone.rect.y + 15.0
                }

                ctx.fillTextRight(note, Vector(playerZone.rect.x + playerZone.rect.width - 7.0, yPos), textBaseline = CanvasTextBaseline.MIDDLE)
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

    private fun renderCardPeek(ctx: CanvasRenderingContext2D) {
        if(! Input.altDown) {
            return
        }
        (Input.getObjectUnderMouse() as? Card)?.let {
            val width = it.rect.width * CARD_PEEK_SCALE
            val height = it.rect.height * CARD_PEEK_SCALE
            renderCard(ctx, Rectangle(it.center - Vector(width, height) / 2.0, width, height), it.getUsedAsset(
                inOtherPlayerZone(it)), false, peek = true)
        }
    }
}
