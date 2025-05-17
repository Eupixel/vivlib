package util

import kotlinx.coroutines.runBlocking
import net.eupixel.vivlib.util.DirectusClient
import net.minestom.server.entity.Player

object Permissions {
    fun getPermissions(player: Player): Array<String> {
        return runBlocking {
            val record = DirectusClient.getData("player_permissions", "uuid", player.uuid.toString(), listOf("permissions"))
            val node = record?.get("permissions")
            val rawPerms = if (node != null && node.isArray) {
                node.map { it.asText() }.also { println("rawPerms=$it") }
            } else {
                DirectusClient.createItem("player_permissions", mapOf("uuid" to player.uuid.toString(), "player_uuid" to player.uuid.toString(), "permissions" to listOf("group.default")))
                listOf("group.default")
            }
            val visited = mutableSetOf<String>()
            val result = mutableListOf<String>()
            suspend fun collect(perm: String) {
                if (perm.startsWith("group.")) {
                    val name = perm.substringAfter("group.")
                    if (visited.add(name)) {
                        val grpNode = DirectusClient.getData("group_permissions", "name", name, listOf("permissions"))
                        val permsList = grpNode?.get("permissions")?.takeIf { it.isArray }?.map { it.asText() } ?: emptyList()
                        permsList.forEach { collect(it) }
                    }
                } else {
                    if (!result.contains(perm)) {
                        result.add(perm)
                    }
                }
            }
            rawPerms.forEach { collect(it) }
            println("final result=$result")
            result.toTypedArray()
        }
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

    fun addPermission(player: Player, permission: String) {
        runBlocking {
            val record = DirectusClient.getData("player_permissions", "uuid", player.uuid.toString(), listOf("uuid", "permissions"))
            if (record == null) {
                DirectusClient.createItem("player_permissions", mapOf("uuid" to player.uuid.toString(), "player_uuid" to player.uuid.toString(), "permissions" to listOf(permission)))
            } else {
                val id = record.get("uuid").asText()
                val current = record.get("permissions")?.map { it.asText() }?.toMutableList() ?: mutableListOf()
                if (!current.contains(permission)) {
                    current.add(permission)
                    DirectusClient.updateItem("player_permissions", id, mapOf("permissions" to current))
                }
            }
        }
    }

    fun removePermission(player: Player, permission: String) {
        runBlocking {
            val record = DirectusClient.getData("player_permissions", "uuid", player.uuid.toString(), listOf("uuid", "permissions"))
            if (record != null) {
                val id = record.get("uuid").asText()
                val current = record.get("permissions")?.map { it.asText() }?.toMutableList() ?: mutableListOf()
                if (current.contains(permission)) {
                    current.remove(permission)
                    DirectusClient.updateItem("player_permissions", id, mapOf("permissions" to current))
                }
            }
        }
    }

    fun removeItem(collection: String, id: String) {
        runBlocking {
            println("removeItem $collection/$id")
            DirectusClient.deleteItem(collection, id)
        }
    }
}