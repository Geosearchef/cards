package framework.rendering

import CardSimulatorClient
import kotlinx.browser.window
import org.w3c.dom.Image
import util.Util

class Image(val imageSrc: String) {

    var loadingStartTime = Util.currentTimeMillis()
    var loaded = false
        private set

    var wrappedImage = org.w3c.dom.Image()
    var mipmaps: MutableList<Image> = ArrayList()

    init {
        loadImage()
    }

    fun loadImage() {
        wrappedImage.src = ""
        wrappedImage.src = imageSrc
        wrappedImage.onload = { this.onImageLoaded() }
        loadingStartTime = Util.currentTimeMillis()

        window.setTimeout({
            if(!loaded) {
                console.log("Image download timed out, reloading $imageSrc")
                loadImage()
            }
        }, 5000)
    }

    fun onImageLoaded() {
        console.log("framework.rendering.Image loaded: $imageSrc")

        mipmaps.add(wrappedImage)
        while(mipmaps.last().width > 20) {
            mipmaps.add(Util.resampleImageToHalfSize(mipmaps.last()))
        }

        loaded = true

        CardSimulatorClient.requestRender()
    }

    fun getMipmap(width: Int, height: Int) = mipmaps.first { it.complete&& it.width > width && it.height > height }
}