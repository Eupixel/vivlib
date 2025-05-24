// Example

import net.eupixel.core.DirectusClient

fun main() {
    DirectusClient.initFromEnv()
    val rawMOTD = DirectusClient.getData("global_values", "name", "motd", "data").toString()
    println("'$rawMOTD'")
}