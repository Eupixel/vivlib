package util

import kotlinx.coroutines.runBlocking
import net.eupixel.vivlib.util.DirectusClient
import net.minestom.server.entity.Player

object Permissions {
    fun getPermissions(player: Player): Array<String> {
        return runBlocking {
            val record = DirectusClient.getData(
                "player_permissions",
                "uuid",
                player.uuid.toString(),
                listOf("permissions")
            )
            val rawPerms = if (record?.get("permissions")?.isArray == true) {
                record.get("permissions").map { it.asText() }
            } else {
                DirectusClient.createItem(
                    "player_permissions",
                    mapOf(
                        "uuid" to player.uuid.toString(),
                        "permissions" to listOf("group.default")
                    )
                )
                listOf("group.default")
            }
            val visited = mutableSetOf<String>()
            val result = mutableListOf<String>()
            suspend fun collect(perm: String) {
                if (perm.startsWith("group.")) {
                    val name = perm.substringAfter("group.")
                    if (visited.add(name)) {
                        val grp = DirectusClient.getData(
                            "group_permissions",
                            "name",
                            name,
                            listOf("permissions")
                        )?.get("permissions")
                        if (grp?.isArray == true) {
                            grp.map { it.asText() }.forEach { collect(it) }
                        }
                    }
                } else {
                    if (!result.contains(perm)) {
                        result.add(perm)
                    }
                }
            }
            rawPerms.forEach { collect(it) }
            result.toTypedArray()
        }
    }

    fun hasPermission(player: Player, permission: String): Boolean {
        return getPermissions(player).contains(permission)
    }

    fun getPrefix(player: Player): String {
        val perms = getPermissions(player)
        if (perms.isEmpty()) {
            return ""
        }
        return perms.filter { it.startsWith("prefix:") }
            .minByOrNull {
                val weightStr = it.substringAfter("prefix:").substringBefore(":")
                weightStr.toIntOrNull() ?: Int.MAX_VALUE
            }
            ?.substringAfter("prefix:")
            ?.substringAfter(":")
            ?: ""
    }
}