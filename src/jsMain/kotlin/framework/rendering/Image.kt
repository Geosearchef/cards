package framework.rendering

import CardSimulatorClient
import kotlinx.browser.window
import util.Util

class Image(val imageSrc: String) {

    var loadingStartTime = Util.currentTimeMillis()
    var loaded = false
        private set

    var wrappedImage = org.w3c.dom.Image()

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
        loaded = true

        CardSimulatorClient.requestRender()
    }
}