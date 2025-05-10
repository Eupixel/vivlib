package net.eupixel.vivlib.util

import okhttp3.Authenticator
import okhttp3.Authenticator.Companion.NONE
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.net.ssl.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.PasswordAuthentication
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

class WebDavClient {
    private val baseUrl: String
    private val username: String
    private val password: String
    private val client: OkHttpClient

    init {
        val host = System.getenv("WEBDAV_HOST")
            ?: throw IllegalStateException("WEBDAV_HOST not set")
        baseUrl = when {
            host.startsWith("http://") || host.startsWith("https://") -> host.trimEnd('/')
            else -> "http://$host"
        }
        username = System.getenv("WEBDAV_USER")
            ?: throw IllegalStateException("WEBDAV_USER not set")
        password = System.getenv("WEBDAV_PASS")
            ?: throw IllegalStateException("WEBDAV_PASS not set")
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }
        client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .authenticator(object : Authenticator {
                override fun authenticate(route: Route?, response: Response): Request? {
                    val credential = Credentials.basic(username, password)
                    return response.request.newBuilder()
                        .header("Authorization", credential)
                        .build()
                }
            })
            .build()
    }

    suspend fun awaitValue(key: String): String? {
        val method = "GET"
        val uri = "/$key"
        val url = "$baseUrl$uri"
        val noAuthClient = client.newBuilder()
            .authenticator(NONE)
            .build()
        val initialRequest = Request.Builder()
            .url(url)
            .get()
            .build()
        noAuthClient.awaitCall(initialRequest).use { resp1 ->
            if (resp1.isSuccessful) return resp1.body?.string()
            if (resp1.code != 401) return null
            val header = resp1.header("WWW-Authenticate") ?: return null
            val params = mutableMapOf<String, String>()
            Regex("""(\w+)="([^"]*)"""").findAll(header).forEach { match ->
                params[match.groupValues[1]] = match.groupValues[2]
            }
            val realm = params["realm"] ?: return null
            val nonce = params["nonce"] ?: return null
            val qop = params["qop"] ?: "auth"
            val opaque = params["opaque"]

            fun md5(s: String): String {
                val md = MessageDigest.getInstance("MD5")
                return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
            }
            val ha1 = md5("$username:$realm:$password")
            val ha2 = md5("$method:$uri")
            val nc = "00000001"
            val cnonce = UUID.randomUUID().toString().replace("-", "")
            val responseDigest = md5("$ha1:$nonce:$nc:$cnonce:$qop:$ha2")
            val authValue = buildString {
                append("Digest username=\"").append(username)
                append("\", realm=\"").append(realm)
                append("\", nonce=\"").append(nonce)
                append("\", uri=\"").append(uri)
                append("\", qop=").append(qop)
                append(", nc=").append(nc)
                append(", cnonce=\"").append(cnonce)
                append("\", response=\"").append(responseDigest)
                opaque?.let { append("\", opaque=\"").append(it) }
                append("\"")
            }
            val authRequest = initialRequest.newBuilder()
                .header("Authorization", authValue)
                .build()
            noAuthClient.awaitCall(authRequest).use { resp2 ->
                return if (resp2.isSuccessful) resp2.body?.string() else null
            }
        }
    }

    @Suppress("DEPRECATION")
    fun awaitFile(key: String): ByteArray? {
        java.net.Authenticator.setDefault(object : java.net.Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(username, password.toCharArray())
        })
        HttpURLConnection.setFollowRedirects(true)
        val url = URL(baseUrl.trimEnd('/') + "/$key")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connect()
        if (conn.responseCode !in 200..299) throw IllegalStateException("Fallback download failed: HTTP ${conn.responseCode}")
        return conn.inputStream.use { it.readAllBytes() }
    }

    fun unzip(zipFile: String, outputDir: String) {
        ZipInputStream(File(zipFile).inputStream()).use { zin ->
            val targetDir = File(outputDir).absoluteFile
            var entry = zin.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfter('/', "")
                if (name.isNotEmpty()) {
                    val outFile = File(targetDir, name)
                    if (entry.isDirectory) outFile.mkdirs() else {
                        outFile.parentFile!!.mkdirs()
                        FileOutputStream(outFile).use { os -> zin.copyTo(os) }
                    }
                }
                zin.closeEntry()
                entry = zin.nextEntry
            }
        }
    }

    private suspend fun OkHttpClient.awaitCall(request: Request): Response {
        return suspendCancellableCoroutine { cont ->
            val call = newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!cont.isCancelled) cont.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    cont.resume(response)
                }
            })
        }
    }
}