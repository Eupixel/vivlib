package net.eupixel.vivlib.util

import net.eupixel.vivlib.core.DirectusClient
import java.util.UUID

object Permissions {
    private val cache = mutableMapOf<UUID, List<String>>()

    fun getPermissions(uuid: UUID): List<String> =
        cache[uuid] ?: loadPermissions(uuid).also { cache[uuid] = it }

    fun hasPermission(uuid: UUID, permission: String): Boolean =
        getPermissions(uuid).contains(permission)

    fun refreshAll() {
        cache.keys.forEach { loadPermissions(it).also { perms -> cache[it] = perms } }
    }

    fun refresh(uuid: UUID) {
        cache[uuid] = loadPermissions(uuid)
    }

    fun getPrefix(uuid: UUID): String =
        getPermissions(uuid)
            .filter { it.startsWith("prefix:") }
            .minByOrNull {
                it.substringAfter("prefix:")
                    .substringBefore(":")
                    .toIntOrNull() ?: Int.MAX_VALUE
            }
            ?.substringAfter("prefix:")
            ?.substringAfter(":")
            ?.removeSuffix(":")
            .orEmpty()

    private fun loadPermissions(uuid: UUID): List<String> {
        val permsFromDb = DirectusClient.getFields(
            "player_permissions",
            "uuid",
            uuid.toString(),
            "permissions"
        ).mapNotNull { it["permission"]?.asText() }
            .toMutableList()
        if (permsFromDb.isEmpty()) {
            DirectusClient.createItem(
                "player_permissions",
                mapOf(
                    "uuid" to uuid.toString(),
                    "permissions" to listOf(mapOf("permission" to "group.default"))
                )
            )
            permsFromDb += "group.default"
        }
        return permsFromDb.flatMap { perm ->
            if (perm.startsWith("group.")) {
                val groupName = perm.removePrefix("group.")
                DirectusClient.getFields(
                    "group_permissions",
                    "name",
                    groupName,
                    "permissions"
                ).mapNotNull { it["permission"]?.asText() }
            } else {
                listOf(perm)
            }
        }.distinct()
    }
}