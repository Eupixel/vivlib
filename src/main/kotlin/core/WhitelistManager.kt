package net.eupixel.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.eupixel.model.WhitelistEntry
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.UUID.fromString
import kotlin.collections.set

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
        println("Adding $uuid: IP:$ip TTL:$ttl")
        whitelist[fromString(uuid)] = WhitelistEntry(ip, ttl, timestamp)
    }

    fun handle(event: AsyncPlayerConfigurationEvent) {
        println("Checking whitelist for ${event.player.username}, I currently have ${whitelist.size} whitelists.")
        println(event.player.uuid.toString())
        println(event.player.playerConnection.remoteAddress)
        var allow = false
        val entry = whitelist[event.player.uuid]
        if(entry != null) {
            allow = event.player.playerConnection.remoteAddress.toString() == entry.ip
        }
        if (!allow) {
            event.player.kick("You are not allowed here. Further joins will result in a ban.")
        }
        whitelist.remove(event.player.uuid)
    }
}