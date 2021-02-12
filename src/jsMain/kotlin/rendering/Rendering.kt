package rendering

import CardSimulatorClient
import assets.AssetManager
import framework.rendering.color
import framework.rendering.fillCircle
import framework.rendering.fillRect
import framework.rendering.setIdentityMatrix
import framework.scene.Scene.SceneRenderer
import game.Card
import game.Game
import game.Table
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HIGH
import org.w3c.dom.ImageSmoothingQuality
import util.math.Rectangle

object Rendering : SceneRenderer {

    private const val PLAYER_CURSOR_RADIUS = 8.0
    private const val CARD_OUTLINE_COLOR = "#222222"
    private const val CARD_OUTLINE_WIDTH = 2.0

    override fun render(ctx: CanvasRenderingContext2D) {
        renderTable(ctx)
    }

    fun renderTable(ctx: CanvasRenderingContext2D) {
        ctx.scale(Table.scale, Table.scale)
        ctx.translate(Table.offset.x, Table.offset.y)

        ctx.color("#AAAAAA")
        ctx.fillRect(150.0, 300.0, 200.0, 200.0)

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

        renderGameObjects(ctx)

        renderPlayerCursors(ctx)

        ctx.setIdentityMatrix()
    }

    fun renderPlayerCursors(ctx: CanvasRenderingContext2D) {
        Game.players.filter { it != CardSimulatorClient.username }.forEach { player ->
            Table.PlayerCursors.renderedCursorPositionByPlayer[player]?.let { cursor ->
                ctx.color(Game.getPlayerColor(player) ?: "444444")
                ctx.fillCircle(cursor, PLAYER_CURSOR_RADIUS)
            }
        }
    }

    fun renderGameObjects(ctx: CanvasRenderingContext2D) {
        ctx.imageSmoothingEnabled = true
        ctx.imageSmoothingQuality = ImageSmoothingQuality.HIGH

        Game.gameObjects.forEach {
            val asset = if(it.flipped) it.backAsset else it.frontAsset
            when(it) {
                is Card -> {
                    if(asset != null) {
                        AssetManager.get(asset)?.wrappedImage?.let { image ->
                            ctx.drawImage(image, it.pos.x, it.pos.y, it.size.x, it.size.y)
                        }

                        ctx.color(CARD_OUTLINE_COLOR)
                        ctx.lineWidth = CARD_OUTLINE_WIDTH
                        ctx.beginPath()
                        ctx.rect(it.pos.x, it.pos.y, it.size.x, it.size.y)
                        ctx.stroke()
                        ctx.lineWidth = 1.0
                    } else {
                        ctx.color("#333333")
                        ctx.fillRect(Rectangle(it.pos, it.size.x, it.size.y))
                    }

                }

                else -> console.log("Couldn't render GameObject of unknown type")
            }
        }
    }
}