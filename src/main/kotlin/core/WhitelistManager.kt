package net.eupixel.vivlib.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.eupixel.vivlib.model.WhitelistEntry
import net.kyori.adventure.text.minimessage.MiniMessage.miniMessage
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import java.time.Duration
import java.time.Instant
import java.util.UUID

object WhitelistManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val whitelist = mutableMapOf<UUID, WhitelistEntry>()

    fun start() {
        scope.launch {
            while (isActive) {
                val now = Instant.now()
                whitelist.entries.forEach {
                    if(Duration.between(it.value.timestamp, now) > Duration.ofSeconds(it.value.ttl.toLong())) {
                        println("Removing ${it.key} from whitelist!")
                        whitelist.remove(it.key)
                    }
                }
                delay(1000L)
            }
        }
    }

    fun add(uuid: String, ip: String, ttl: Int, timestamp: Instant) {
        whitelist[UUID.fromString(uuid)] = WhitelistEntry(formatIP(ip), ttl, timestamp)
    }

    fun handle(event: AsyncPlayerConfigurationEvent) {
        val entry = whitelist[event.player.uuid]
        val allow = entry != null && formatIP(event.player.playerConnection.remoteAddress.toString()) == entry.ip
        if (!allow) {
            event.player.kick(miniMessage().deserialize(DBTranslator.get("join_deny", event.player.locale)))
        }
        whitelist.remove(event.player.uuid)
    }

    fun formatIP(ip: String): String {
        return ip.substringAfter("/").substringBefore(":")
    }
}