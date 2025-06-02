// Example

import net.eupixel.core.DBTranslator
import net.eupixel.core.DirectusClient
import java.util.Locale

fun main() {
    DirectusClient.initFromEnv()
    DBTranslator.loadFromDB()
    val test = DBTranslator.translate("whereami", Locale.US)
    println(test)
}