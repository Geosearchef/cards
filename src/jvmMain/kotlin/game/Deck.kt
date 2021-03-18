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

            for(k in 0..8) {
                for(i in 1..13) {
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

            for(k in 0..5) {
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
                    "CardAB.png"
                )
            )
        }
    };

    open fun spawn() {}
}