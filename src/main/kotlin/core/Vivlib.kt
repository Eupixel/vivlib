package core

import net.eupixel.vivlib.command.WhereAmICommand
import net.eupixel.vivlib.core.Messenger
import net.eupixel.vivlib.core.WhitelistManager
import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent

object Vivlib {
    fun init() {
        Messenger.addWhiteListListener()
        MinecraftServer.getCommandManager().register(WhereAmICommand())
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent::class.java) { event -> WhitelistManager.handle(event) }
    }
}