package net.eupixel.util

import net.eupixel.vivlib.util.Permissions
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.kyori.adventure.text.minimessage.MiniMessage.miniMessage
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import kotlin.apply
import kotlin.collections.filter
import kotlin.collections.forEach

object PrefixLoader {
    fun loadPrefix(player: Player) {
        player.passengers.filter { it.entityType == EntityType.TEXT_DISPLAY }.forEach {
            it.remove()
        }
        val name = Permissions.getPrefix(player.uuid) + " " + player.username
        val component = miniMessage().deserialize(name)
        val display = Entity(EntityType.TEXT_DISPLAY).apply {
            setInstance(player.instance, player.position)
            setNoGravity(true)
            val meta = entityMeta as TextDisplayMeta
            meta.text = component
            meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
            meta.translation = Pos(0.0, 0.25, 0.0)
            meta.isHasGlowingEffect = true
            meta.backgroundColor = 0x00000000
        }
        player.displayName = component
        player.addPassenger(display)
    }
}