package util

import kotlinx.coroutines.runBlocking
import net.eupixel.vivlib.util.DirectusClient
import net.minestom.server.entity.Player

object Permissions {
    fun getPermissions(player: Player): Array<String> = runBlocking {
        val permsNode = DirectusClient
            .getData("player_permissions", "uuid", player.uuid.toString(), listOf("permissions"))
            ?.get("permissions")
        val rawPerms = if (permsNode != null && permsNode.isArray) permsNode.map { it.asText() } else emptyList()
        val directPerms = rawPerms.filterNot { it.startsWith("group.") }
        val visitedGroups = mutableSetOf<String>()
        val groupPerms = mutableListOf<String>()
        suspend fun resolveGroup(groupName: String) {
            if (!visitedGroups.add(groupName)) return
            val node = DirectusClient
                .getData("group_permissions", "name", groupName, listOf("permissions"))
                ?.get("permissions")
            if (node != null && node.isArray) {
                val perms = node.map { it.asText() }
                groupPerms += perms.filterNot { it.startsWith("group.") }
                for (sub in perms.filter { it.startsWith("group.") }.map { it.substringAfter("group.") }) {
                    resolveGroup(sub)
                }
            }
        }
        for (group in rawPerms.filter { it.startsWith("group.") }.map { it.substringAfter("group.") }) {
            resolveGroup(group)
        }
        (directPerms + groupPerms).distinct().toTypedArray()
    }

    fun getPrefix(player: Player): String {
        val permissions = getPermissions(player)
        val filterd = permissions.filter { it.startsWith("prefix:") }
        return if (filterd.isNotEmpty()) {
            filterd[0].removePrefix("prefix:")
        } else {
            ""
        }
    }
}