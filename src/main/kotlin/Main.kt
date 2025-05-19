// Example

import net.eupixel.core.DirectusClient
import net.eupixel.core.DBTranslator
import net.eupixel.util.PrefixLoader
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block

fun main() {
    DirectusClient.initFromEnv()

    val translator = DBTranslator(arrayOf("welcome_message", "flight_state", "prefix"))

    val server = MinecraftServer.init()
    val instance = MinecraftServer.getInstanceManager()
        .createInstanceContainer()
        .apply {
            setGenerator { unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK) }
            setChunkSupplier(::LightingChunk)
        }

    MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        event.spawningInstance = instance
        event.player.respawnPoint = Pos(0.0, 42.0, 0.0)
    }

    MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent::class.java) { event ->
        event.player.sendMessage(MiniMessage.miniMessage().deserialize(translator.get("welcome_message", event.player.locale).replace("<player>", event.player.username)))
        PrefixLoader.loadPrefix(event.player)
    }

    MojangAuth.init()
    server.start("0.0.0.0", 25565)
}