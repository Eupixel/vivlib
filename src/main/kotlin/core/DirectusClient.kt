package net.eupixel.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
        init(
            System.getenv("HOST") ?: error("HOST not set"),
            System.getenv("TOKEN") ?: error("TOKEN not set")
        )
    }

    fun createItem(collection: String, payload: Any): JsonNode? = runBlocking {
        val url = "$host/items/$collection"
        val jsonPayload = mapper.writeValueAsString(payload)
        val req = Request.Builder()
            .url(url)
            .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("Create failed (${res.code}): ${res.body?.string()}")
            mapper.readTree(res.body!!.string())["data"]
        }
    }

    fun getItems(collection: String, filterField: String, filterValue: String, fields: List<String>): List<JsonNode> = runBlocking {
        val url =
            "$host/items/$collection?filter[$filterField][_eq]=$filterValue&fields=${fields.joinToString(",")}"
        val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@runBlocking emptyList()
            val root = mapper.readTree(res.body!!.string())
            root["data"].map { it }
        }
    }

    fun getData(collection: String, filterField: String, filterValue: String, field: String): String? = runBlocking {
        val url =
            "$host/items/$collection?filter[$filterField][_eq]=$filterValue&fields=$field"
        val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@runBlocking null
            val root = mapper.readTree(res.body!!.string())
            root["data"].firstOrNull()?.get(field)?.asText()
        }
    }

    fun downloadFile(
        collection: String,
        filterField: String,
        filterValue: String,
        fileField: String,
        outputPath: String
    ) = runBlocking {
        val id = mapper.readTree(
            client.newCall(
                Request.Builder()
                    .url("$host/items/$collection?filter[$filterField][_eq]=$filterValue&fields=$fileField")
                    .header("Authorization", "Bearer $token")
                    .build()
            ).execute().use { res ->
                if (!res.isSuccessful) return@runBlocking false
                res.body!!.string()
            }
        )["data"].firstOrNull()?.get(fileField)?.asText() ?: return@runBlocking false
        client.newCall(
            Request.Builder()
                .url("$host/assets/$id?download=true")
                .header("Authorization", "Bearer $token")
                .build()
        ).execute().use { res ->
            if (!res.isSuccessful) return@runBlocking false
            res.body!!.byteStream().use { input ->
                File(outputPath).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        true
    }

    fun getLocalizedMap(
        collection: String,
        filterField: String,
        filterValue: String,
        arrayField: String,
        localeFieldName: String  = "locale",
        textFieldName: String    = "message"
    ): Map<String, String>? = runBlocking {
        val url =
            "$host/items/$collection?filter[$filterField][_eq]=$filterValue&fields=$arrayField"
        val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@runBlocking null
            val root = mapper.readTree(res.body!!.string())
            val node = root["data"].firstOrNull() ?: return@runBlocking null
            node[arrayField]
                ?.mapNotNull { elem ->
                    val locale = elem[localeFieldName]?.asText()
                    val text = elem[textFieldName]?.asText()
                    if (locale != null && text != null) locale to text else null
                }
                ?.toMap()
        }
    }

    fun listItems(collection: String, field: String): List<String> = runBlocking {
        val url = "$host/items/$collection?fields=$field"
        val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@runBlocking emptyList()
            val root = mapper.readTree(res.body!!.string())
            root["data"].mapNotNull { it[field]?.asText() }
        }
    }
}