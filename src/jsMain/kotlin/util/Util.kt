package util

import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import kotlin.js.Date
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

actual object Util {
    actual fun currentTimeMillis() = Date().getTime().toLong()

    fun resampleImageToHalfSize(image: Image): Image {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        val ctx = canvas.getContext("2d")!! as CanvasRenderingContext2D

        canvas.width = floor(image.width / 2.0).toInt()
        canvas.height = floor(image.height / 2.0).toInt()

        ctx.drawImage(image, 0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())

        val resizedImage = Image()
        resizedImage.src = canvas.toDataURL("image/png")
        return resizedImage
    }
}

fun Double.toDecimals(n: Int): String {
    val stringified = this.toDecimalsAsDouble(n).toString()
    val digitsAfterSeparator = stringified.split('.').getOrNull(1)?.length ?: 0
    return stringified + (if(digitsAfterSeparator == 0) "." else "") + "0".repeat(n - digitsAfterSeparator)
}

fun Double.toDecimalsAsDouble(n: Int) = (this * 10.0.pow(n)).roundToInt().toDouble() / 10.0.pow(n)