package net.eupixel.vivlib.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

object DirectusClient {
    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()
    private var host = ""
    private var token = ""

    fun init(host: String, token: String) {
        this.host = host.trimEnd('/')
        this.token = token
    }

    fun initFromEnv() {
        init(
            System.getenv("HOST") ?: error("HOST not set"),
            System.getenv("TOKEN") ?: error("TOKEN not set")
        )
    }

    suspend fun downloadWorld(name: String): Boolean = withContext(Dispatchers.IO) {
        val worldReq = Request.Builder()
            .url("$host/items/worlds?filter[name][_eq]=$name&fields=world_data")
            .header("Authorization", "Bearer $token")
            .build()
        val fileId = client.newCall(worldReq).execute().use { res ->
            if (!res.isSuccessful) return@withContext false
            val json = mapper.readTree(res.body!!.string())
            val item = json["data"].firstOrNull() ?: return@withContext false
            item["world_data"]?.asText() ?: return@withContext false
        }
        val assetReq = Request.Builder()
            .url("$host/assets/$fileId")
            .header("Authorization", "Bearer $token")
            .build()
        val outFile = File("$name.zip")
        client.newCall(assetReq).execute().use { res ->
            if (!res.isSuccessful) return@withContext false
            res.body!!.byteStream().use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        true
    }

    suspend fun getData(collection: String, filterField: String, filterValue: String, fields: List<String>): JsonNode? = withContext(Dispatchers.IO) {
        val url = "$host/items/$collection?filter[$filterField][_eq]=$filterValue&fields=${fields.joinToString(",")}"
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@withContext null
            val body = res.body!!.string()
            mapper.readTree(body)["data"].firstOrNull()
        }
    }

    suspend fun createItem(collection: String, payload: Any): JsonNode? = withContext(Dispatchers.IO) {
        val url = "$host/items/$collection"
        val jsonPayload = mapper.writeValueAsString(mapOf("data" to payload))
        val req = Request.Builder()
            .url(url)
            .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@withContext null
            val root = mapper.readTree(res.body!!.string())
            root["data"]
        }
    }

    suspend fun updateItem(collection: String, id: String, payload: Any): JsonNode? = withContext(Dispatchers.IO) {
        val url = "$host/items/$collection/$id"
        val jsonPayload = mapper.writeValueAsString(mapOf("data" to payload))
        val req = Request.Builder()
            .url(url)
            .patch(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@withContext null
            val root = mapper.readTree(res.body!!.string())
            root["data"]
        }
    }

    suspend fun deleteItem(collection: String, filterField: String, filterValue: String): Boolean = withContext(Dispatchers.IO) {
        val findUrl = "$host/items/$collection?filter[$filterField][_eq]=$filterValue&fields=uuid"
        val record = client.newCall(Request.Builder().url(findUrl).header("Authorization", "Bearer $token").build())
            .execute().use { res ->
                val body = res.body!!.string()
                if (!res.isSuccessful) return@withContext false
                mapper.readTree(body)["data"].firstOrNull()?.get("uuid")?.asText()
            } ?: return@withContext false
        val deleteUrl = "$host/items/$collection/$record"
        val delRes = client.newCall(Request.Builder().url(deleteUrl).delete().header("Authorization", "Bearer $token").build())
            .execute().use { res ->
                res.isSuccessful
            }
        delRes
    }
}