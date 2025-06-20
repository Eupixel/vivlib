package net.eupixel.vivlib.core

import net.eupixel.vivlib.command.WhereAmICommand
import net.eupixel.vivlib.util.Permissions
import net.eupixel.vivlib.util.PrefixLoader.loadPrefix
import net.kyori.adventure.text.minimessage.MiniMessage.miniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent

object Vivlib {
    fun init() {
        WhitelistManager.start()
        DirectusClient.initFromEnv()
        Config.init()
        DBTranslator.loadFromDB()
        Messenger.bind("0.0.0.0", 2905)
        Messenger.registerTarget("entrypoint", "entrypoint", 2905)
        Messenger.addBaseListener()
        MinecraftServer.getCommandManager().register(WhereAmICommand())
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent::class.java) { event -> WhitelistManager.handle(event) }
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent::class.java) { event -> loadPrefix(event.player) }
        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent::class.java) { event -> event.player.passengers.forEach { it.remove() } }
        MinecraftServer.getGlobalEventHandler().addListener(PlayerChatEvent::class.java) { event ->
            val format = Config.chatFormat
            val message = format
                .replace("<player_prefix>", Permissions.getPrefix(event.player.uuid))
                .replace("<player>", event.player.username)
                .replace("<message>", event.rawMessage)
            event.formattedMessage = miniMessage().deserialize(message)
        }
    }

    fun reload() {
        Config.init()
    }
}