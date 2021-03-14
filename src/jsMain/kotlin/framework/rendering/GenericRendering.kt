package framework.rendering

import CardSimulatorClient
import framework.scene.SceneManager
import org.w3c.dom.*
import util.toDecimals
import websocket.WebsocketClient

/**
 * The renderer for all scenes
 */
object GenericRendering {

    private var averageFrameTime = -1.0;

    lateinit var ctx: CanvasRenderingContext2D
    var width = 0.0
    var height = 0.0


    fun render(delta: Double, canvas: HTMLCanvasElement) {
        canvas.resizeCanvas()

        if(!CardSimulatorClient.renderRequested && !CardSimulatorClient.continousRendering) {
            return
        }
        CardSimulatorClient.renderRequested = false

        ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        width = canvas.width.toDouble()
        height = canvas.height.toDouble()
        ctx.clearRect(0.0, 0.0, width, height)

        SceneManager.currentScene.renderer.render(ctx)
        SceneManager.currentScene.uiManager.getUI().render(ctx)

        // render frametime
        ctx.fillStyle = "#000000"
        ctx.font = "10px sans-serif"
        ctx.textAlign = CanvasTextAlign.RIGHT
        averageFrameTime = if(averageFrameTime == -1.0) delta else averageFrameTime * 0.95 + delta * 0.05;
        ctx.fillText(
                "Latency: ${WebsocketClient.lastRTT.toInt()} ms    " +
                        "In: ${WebsocketClient.receivedMessages} packets (${(WebsocketClient.receivedBytes / 1000.0).toDecimals(1)} KB)    " +
                        "Out: ${WebsocketClient.transmittedMessages} packets (${(WebsocketClient.transmittedBytes / 1000.0).toDecimals(1)} KB)    " +
                        "Frame Time: ${averageFrameTime.toDecimals(3)} s  (${(1.0 / averageFrameTime).toDecimals(1)} fps)",
                width - 3.0,
                height - 3.0
        )
        ctx.textAlign = CanvasTextAlign.LEFT
        ctx.font = "10px sans-serif"
    }
}