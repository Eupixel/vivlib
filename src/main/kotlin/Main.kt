// Example

import net.eupixel.core.DirectusClient

fun main() {
    DirectusClient.initFromEnv()
    DirectusClient.downloadFile("icons", "name", "server", "icon", "icon.png")
}