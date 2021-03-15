package game

import util.Util
import kotlin.math.pow

/**
 * the client extensions are referenced from the game objects themselves and contained on client and server
 * but only used on the client, they are not serialized
 * This avoids the need of having to create maps for every additional client property stored for a gameObject
 */

open class GameObjectClientExtension(val gameObject: GameObject) {

    var serverPos = gameObject.pos
    var grabbed: Boolean = false
    var lastGrabTime: Long = 0

    open fun update(delta: Double) {
        if(!grabbed && Util.currentTimeMillis() - lastGrabTime > 500) {
            if((gameObject.pos - serverPos).lengthSquared() < 1.0) {
                gameObject.pos = serverPos
            } else {
//                gameObject.pos = (gameObject.pos * 0.80) + (serverPos * 0.2)
                // frame independent (solve equation)
                val p = 0.8
                val d = delta * 190.0

                // (p.pow(d) - 1.0) / (p - 1.0) = sum p^(k-1),k=1 to n
                gameObject.pos = (gameObject.pos * p.pow(d)) + (serverPos * (1.0 - p) * (p.pow(d) - 1.0) / (p - 1.0))
            }
        }
    }
}