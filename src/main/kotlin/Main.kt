// Example

import kotlinx.coroutines.runBlocking
import net.eupixel.core.WhitelistManager
import java.time.Instant

fun main() = runBlocking {
    WhitelistManager.start()
    WhitelistManager.add("74401e14-f69c-48f3-b393-64be488dff8f", "/172.18.0.1:58000&30", 30, Instant.now())
}