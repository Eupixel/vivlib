package net.eupixel.vivlib.util

import okhttp3.OkHttpClient
import okhttp3.Request
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DirectusClient(
    private val host: String,
    private val token: String
) {
    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()

    suspend fun downloadWorld(name: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$host/items/worlds?filter[name][_eq]=$name&fields=world_data,spawn_position")
            .header("Authorization", "Bearer $token")
            .build()
        val (fileId, spawn) = client.newCall(request).execute().use { res ->
            if (!res.isSuccessful) return@withContext false to null
            val json = mapper.readTree(res.body?.string())
            val item = json["data"].firstOrNull() ?: return@withContext false to null
            val file = item["world_data"]?.asText() ?: return@withContext false to null
            val spawnPos = item["spawn_position"]?.asText()
            file to spawnPos
        }
        val downloadRequest = Request.Builder()
            .url("$host/assets/$fileId")
            .header("Authorization", "Bearer $token")
            .build()
        val target = File("$name.zip")
        client.newCall(downloadRequest).execute().use { res ->
            if (!res.isSuccessful) return@withContext false to spawn
            res.body?.byteStream()?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false to spawn
        }
        true to spawn
    }
}