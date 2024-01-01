package game

import game.objects.Card
import game.objects.NonStackableGameObject
import game.objects.TokenObject
import util.math.Vector

enum class Deck(val identifier: String) {
    PASSO("passo") {
        override fun spawn() {
            val gridSize = Vector(50.0, 50.0)

            for(x in 1..5) {
                for(y in 1..5) {
                    GameManager.addGameObject(
                        NonStackableGameObject(
                            Vector(x * gridSize.x - 525, y * gridSize.y - 175),
                            gridSize,
                            "QuadraticPlate.png",
                            "QuadraticPlateBack.png"
                        )
                    )
                }
            }

            for(x in 1..5) {
                GameManager.addGameObject(
                    NonStackableGameObject(
                        Vector(x * gridSize.x - 525, 1 * gridSize.y - 175),
                        gridSize,
                        "RedPiece.png",
                        "RedPieceBorder.png"
                    )
                )
                GameManager.addGameObject(
                    NonStackableGameObject(
                        Vector(x * gridSize.x - 525, 5 * gridSize.y - 175),
                        gridSize,
                        "BlackPiece.png",
                        "BlackPieceBorder.png"
                    )
                )
            }
        }
    },
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
    LOVE_LETTER("loveletter") {
        override fun spawn() {
            val cardSize = Vector(57.0, 57.0 * (1060.0 / 680.0))  // 57.0 x 88.8

            listOf(1, 1, 1, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8).forEachIndexed { k, cardId ->
                        GameManager.addGameObject(
                            Card(
                                Vector(k * (cardSize.x + 0.0) - 500, 0.0),
                                cardSize,
                                "CardLL_${cardId}.jpg",
                                "CardLL_back.jpg"
                            )
                        )
                }

            GameManager.addGameObject(
                Card(
                    Vector(-1.5 * (cardSize.x + 15.0) - 500, 0.0),
                    cardSize,
                    "CardLL_ListOfCards.jpg",
                    "CardLL_ListOfCards.jpg"
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

            GameManager.addGameObject(TokenObject(Vector(-50.0, 0.0), Vector(30.0, 30.0), "#e38629")) // TODO
        }
    },
    PLAYING_CARDS("default") {
        override fun spawn() {
            val cardSize = Vector(57.0, 57.0 * (1060.0 / 680.0))  // 57.0 x 88.8

            listOf("C", "D", "H", "S").forEachIndexed { k, color ->
                listOf("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A").forEachIndexed { i, card_value ->
                    GameManager.addGameObject(
                        Card(
                            Vector((14 - i) * (cardSize.x + 0.0) - 500, k * cardSize.y - 170),
                            cardSize,
                            "playing-cards-${card_value}${color}.png",
                            "playing-cards-back.png"
                        )
                    )
                }
            }
        }
    };

    open fun spawn() {}
}