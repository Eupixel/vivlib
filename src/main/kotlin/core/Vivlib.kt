package core

import net.eupixel.vivlib.command.WhereAmICommand
import net.minestom.server.MinecraftServer

object Vivlib {
    fun init() {
        MinecraftServer.getCommandManager().register(WhereAmICommand())
    }
}