package net.eupixel.vivlib.util

import okhttp3.OkHttpClient
import okhttp3.Request
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DirectusClient {

    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()
    private var host: String = ""
    private var token: String = ""

    fun init(host: String, token: String) {
        this.host = host
        this.token = token
    }

    suspend fun downloadWorld(name: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$host/items/worlds?filter[name][_eq]=$name&fields=world_data,spawn_position")
            .header("Authorization", "Bearer $token")
            .build()
        val (fileId, spawn) = client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@withContext false to null
            val json = mapper.readTree(res.body?.string())
            val item = json["data"].firstOrNull() ?: return@withContext false to null
            val id = item["world_data"]?.asText() ?: return@withContext false to null
            val spawn = item["spawn_position"]?.asText()
            id to spawn
        }
        val zipReq = Request.Builder()
            .url("$host/assets/$fileId")
            .header("Authorization", "Bearer $token")
            .build()
        val file = File("$name.zip")
        client.newCall(zipReq).execute().use { res ->
            if (!res.isSuccessful) return@withContext false to spawn
            res.body?.byteStream()?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext false to spawn
        }
        true to spawn
    }

    suspend fun getSpawnPosition(name: String): String? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$host/items/worlds?filter[name][_eq]=$name&fields=spawn_position")
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@withContext null
            val json = mapper.readTree(res.body?.string())
            val item = json["data"].firstOrNull() ?: return@withContext null
            item["spawn_position"]?.asText()
        }
    }
}