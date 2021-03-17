package util

import CardSimulatorOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.ArrayList


actual object Util {
    fun isRunningFromJar() = CardSimulatorOptions.javaClass.getResource("CardSimulatorOptions.class").toString().startsWith("jar")
    inline fun logger(): Logger = LoggerFactory.getLogger(Class.forName(Thread.currentThread().stackTrace[1].className))

    actual fun currentTimeMillis() = System.currentTimeMillis()

    val random = Random()

    fun <T> shuffleListInPlace(list: MutableList<T>): MutableList<T> {
        val elements = ArrayList(list)
        list.clear()

        while(elements.isNotEmpty()) {
            list.add(elements.removeAt(random.nextInt(elements.size)))
        }
        return list
    }
}
