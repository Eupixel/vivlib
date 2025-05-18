package net.eupixel.vivlib.util

import java.util.UUID

object Permissions {
    suspend fun getPermissions(uuid: UUID): List<String> {
        val perms = DirectusClient.getItems("player_permissions", "uuid", uuid.toString(), listOf("permissions"))
            .firstOrNull()
            ?.get("permissions")
            ?.mapNotNull { it["permission"]?.asText() }
            ?.toMutableList() ?: mutableListOf()
        if (perms.isEmpty()) {
            DirectusClient.createItem("player_permissions", mapOf(
                "uuid" to uuid.toString(),
                "permissions" to listOf(mapOf("permission" to "group.default"))
            ))
            perms.add("group.default")
        }
        val finalPerms = mutableSetOf<String>()
        for (perm in perms) {
            if (perm.startsWith("group.")) {
                val groupPerms = DirectusClient.getItems("group_permissions", "name", perm.removePrefix("group."), listOf("permissions"))
                    .firstOrNull()
                    ?.get("permissions")
                    ?.mapNotNull { it["permission"]?.asText() } ?: emptyList()
                finalPerms.addAll(groupPerms)
            } else {
                finalPerms.add(perm)
            }
        }
        return finalPerms.toList()
    }

    suspend fun hasPermission(uuid: UUID, permission: String): Boolean {
        return getPermissions(uuid).contains(permission)
    }

    suspend fun getPrefix(uuid: UUID): String {
        return getPermissions(uuid)
            .filter { it.startsWith("prefix:") }
            .minByOrNull {
                it.substringAfter("prefix:")
                    .substringBefore(":")
                    .toIntOrNull() ?: Int.MAX_VALUE
            }
            ?.substringAfter("prefix:")
            ?.substringAfter(":")
            ?.removeSuffix(":")
            ?: ""
    }
}