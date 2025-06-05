package net.eupixel.vivlib.command

import net.eupixel.core.DBTranslator
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player
import java.io.File
import java.net.InetAddress
import java.util.Locale
import kotlin.io.readText
import kotlin.runCatching
import kotlin.text.replace
import kotlin.text.trim

class WhereAmICommand : Command("whereami") {
    init {
        setDefaultExecutor { sender, _ ->
            if(sender is Player) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(DBTranslator.get("whereami", sender.locale).replace("<id>", System.getenv("HOSTNAME")
                    ?: runCatching { File("/etc/hostname").readText().trim() }.getOrNull()
                    ?: runCatching { InetAddress.getLocalHost().hostName }.getOrNull()
                    ?: "ID not found")))
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(DBTranslator.get("whereami", Locale.US).replace("<id>", System.getenv("HOSTNAME")
                    ?: runCatching { File("/etc/hostname").readText().trim() }.getOrNull()
                    ?: runCatching { InetAddress.getLocalHost().hostName }.getOrNull()
                    ?: "ID not found")))
            }
        }
    }
}