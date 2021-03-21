package game

import game.objects.Card
import util.math.Vector

enum class Deck(val identifier: String) {
    DEMO("demo") {
        override fun spawn() {
            val cardSize = Vector(57.0, 57.0 * (1060.0 / 680.0))  // 57.0 x 88.8

            for(i in 1..13) {
                GameManager.addGameObject(
                    Card(
                        Vector(i * (cardSize.x + 15.0) - 500, 0.0),
                        cardSize,
                        "CardA$i.png",
                        "CardAB.png"
                    )
                )
            }
        }
    },
    ABLUXXEN("abluxxen") {
        override fun spawn() {
            val cardSize = Vector(57.0, 57.0 * (1060.0 / 680.0))  // 57.0 x 88.8

            for(i in 1..13) {
                for(k in 0..7) {
                    GameManager.addGameObject(
                        Card(
                            Vector(i * (cardSize.x + 15.0) - 500, k * 15.0),
                            cardSize,
                            "CardA$i.png",
                            "CardAB.png"
                        )
                    )
                }
            }

            for(k in 0..4) {
                GameManager.addGameObject(
                    Card(
                        Vector(14.0 * (cardSize.x + 15.0) - 500, k * 15.0),
                        cardSize,
                        "CardAX.png",
                        "CardAB.png"
                    )
                )
            }

            GameManager.addGameObject(
                Card(
                    Vector(-1.5 * (cardSize.x + 15.0) - 500, 0.0),
                    cardSize,
                    "CardAS.png",
                    "CardAS.png"
                )
            )
        }
    },
    WIZARD("wizard") {
        override fun spawn() {
            val cardSize = Vector(57.0, 57.0 * (1060.0 / 680.0))  // 57.0 x 88.8

            listOf("Y", "R", "G", "B").forEachIndexed { k, color ->
                for(i in 1..13) {
                    GameManager.addGameObject(
                        Card(
                            Vector((14 - i) * (cardSize.x + 0.0) - 500, k * cardSize.y - 170),
                            cardSize,
                            "CardW_${color}${i}.jpg",
                            "CardW_B.jpg"
                        )
                    )
                }
            }
            listOf("Y", "R", "G", "B").forEachIndexed { k, color ->
                GameManager.addGameObject(
                    Card(
                        Vector(0 * (cardSize.x + 0.0) - 500, k * cardSize.y - 170),
                        cardSize,
                        "CardW_${color}N.jpg",
                        "CardW_B.jpg"
                    )
                )
            }
            listOf("Y", "R", "G", "B").forEachIndexed { k, color ->
                GameManager.addGameObject(
                    Card(
                        Vector(-1 * (cardSize.x + 0.0) - 500, k * cardSize.y - 170),
                        cardSize,
                        "CardW_${color}Z.jpg",
                        "CardW_B.jpg"
                    )
                )
            }
        }
    };

    open fun spawn() {}
}