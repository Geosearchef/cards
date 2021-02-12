package assets

import framework.rendering.Image

object AssetManager {
    const val ASSETS_BASE_URL = "/asset/"
    var ASSET_TOKEN = ""

    val assetsByName: MutableMap<String, Asset> = HashMap()

    fun get(name: String): Image? {
        val asset = assetsByName[name]
        if(asset != null) {
            if(asset.image.loaded) {
                return asset.image
            } else {
                return null
            }
        } else {
            console.log("Loading asset: " + name)
            assetsByName[name] = Asset(name, Image(ASSETS_BASE_URL + name + "?token=" + ASSET_TOKEN))
            return null
        }
    }

    data class Asset(val name: String, val image: Image)
}