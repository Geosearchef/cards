package util.toolbox

import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO

const val atlasWidth = 10
const val atlasHeight = 7

const val BORDER_RADIUS = 15

fun main(args: Array<String>) {
    val outputDir = Paths.get("./imageOutput")
    if(!Files.exists(outputDir)) {
        Files.createDirectories(outputDir)
    }

    val input = ImageIO.read(Paths.get("./input.png").toFile())

    val cardSizeX = input.width.toDouble() / atlasWidth.toDouble()
    val cardSizeY = input.height.toDouble() / atlasHeight.toDouble()


    val names: MutableList<String> = ArrayList()
    for(color in listOf("Y", "R", "G", "B")) {
        names.add("CardW_${color}Z.jpg")
        names.add("CardW_${color}N.jpg")
        for(i in 13 downTo 1) {
            names.add("CardW_${color}${i}.jpg")
        }
    }



    val nameIter = names.iterator()
    var nameAlternative = 0
    for(y in 0 until atlasHeight) {
        for(x in 0 until atlasWidth) {

            val subImage = input.getSubimage((cardSizeX * x).toInt(),
                (cardSizeY * y).toInt(),
                cardSizeX.toInt(),
                cardSizeY.toInt()
            )


//            for(subX in 0 until subImage.width) {
//                for(subY in 0 until subImage.height) {
//                    if(pow(subX - 5, 2) + pow(subY - 5, 2) < pow(BORDER_RADIUS, 2) && subX < BORDER_RADIUS && subY < BORDER_RADIUS) {
//                        subImage.setRGB(subX, subY, Color(0, 0, 0, 0).rgb)
//                    }
//                }
//            }

            val filename = if(nameIter.hasNext()) nameIter.next() else "CardW_${nameAlternative++}.jpg"
            ImageIO.write(subImage, "JPG", outputDir.resolve(filename).toFile())
            println("Writing $filename...")
        }
    }
}

fun pow(i1: Int, i2: Int): Int {
    if(i2 <= 1) {
        return i1
    } else {
        return pow(i1 * i1, i2-1)
    }
}