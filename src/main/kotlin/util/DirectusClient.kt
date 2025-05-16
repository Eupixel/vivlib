package net.eupixel.vivlib.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object DirectusClient {
    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()
    private var host: String = ""
    private var token: String = ""

    fun init(host: String, token: String) {
        this.host = host.trimEnd('/')
        this.token = token
    }

    fun initFromEnv() {
        val h = System.getenv("HOST") ?: error("Environment variable HOST not set")
        val t = System.getenv("TOKEN") ?: error("Environment variable TOKEN not set")
        init(h, t)
    }

    suspend fun downloadWorld(name: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$host/items/worlds?filter[name][_eq]=$name&fields=world_data,spawn_position")
            .header("Authorization", "Bearer $token")
            .build()
        val (fileId, spawn) = client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@withContext false to null
            val json = mapper.readTree(res.body!!.string())
            val item = json["data"].firstOrNull() ?: return@withContext false to null
            (item["world_data"]?.asText() ?: return@withContext false to null) to item["spawn_position"]?.asText()
        }
        val zipReq = Request.Builder()
            .url("$host/assets/$fileId")
            .header("Authorization", "Bearer $token")
            .build()
        val file = File("$name.zip")
        client.newCall(zipReq).execute().use { res ->
            if (!res.isSuccessful) return@withContext false to spawn
            res.body!!.byteStream().use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        true to spawn
    }

    suspend fun getData(
        collection: String,
        filterField: String,
        filterValue: String,
        fields: List<String>
    ): JsonNode? = withContext(Dispatchers.IO) {
        val fs = fields.joinToString(",")
        val req = Request.Builder()
            .url("$host/items/$collection?filter[$filterField][_eq]=$filterValue&fields=$fs")
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@withContext null
            val json = mapper.readTree(res.body!!.string())
            json["data"].firstOrNull()
        }
    }
}