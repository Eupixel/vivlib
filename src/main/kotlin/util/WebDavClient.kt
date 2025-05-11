package net.eupixel.vivlib.util

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object WebDavClient {
    private val baseUrl: String
    private val username: String
    private val password: String
    private val client: OkHttpClient

    init {
        val hostEnv = System.getenv("WEBDAV_HOST")
            ?: throw IllegalStateException("WEBDAV_HOST not set")
        baseUrl = hostEnv.trimEnd('/').let {
            if (!it.startsWith("http://") && !it.startsWith("https://")) "http://$it" else it
        }
        username = System.getenv("WEBDAV_USER")
            ?: throw IllegalStateException("WEBDAV_USER not set")
        password = System.getenv("WEBDAV_PASS")
            ?: throw IllegalStateException("WEBDAV_PASS not set")
        // Trust all SSL certificates
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAll, SecureRandom())
        }
        client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAll[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .authenticator(DigestAuthenticator(username, password))
            .build()
    }

    suspend fun awaitString(path: String): String? {
        val request = buildRequest(path)
        client.awaitCall(request).use { resp ->
            return resp.body?.string().takeIf { resp.isSuccessful }
        }
    }

    suspend fun awaitBytes(path: String): ByteArray? {
        val request = buildRequest(path)
        client.awaitCall(request).use { resp ->
            return resp.body?.bytes().takeIf { resp.isSuccessful }
        }
    }

    private fun buildRequest(path: String): Request {
        val cleanPath = path.trimStart('/')
        return Request.Builder()
            .url("$baseUrl/$cleanPath")
            .get()
            .build()
    }

    private class DigestAuthenticator(
        private val user: String,
        private val pass: String
    ) : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            if (responseCount(response) >= 3) return null
            val header = response.header("WWW-Authenticate") ?: return null
            val params = parseDigestHeader(header)
            val realm = params["realm"] ?: return null
            val nonce = params["nonce"] ?: return null
            val uri = response.request.url.encodedPath
            val qop = params["qop"] ?: "auth"
            val nc = "00000001"
            val cnonce = UUID.randomUUID().toString().replace("-", "")
            fun md5(s: String) = MessageDigest.getInstance("MD5")
                .digest(s.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val ha1 = md5("$user:$realm:$pass")
            val ha2 = md5("${response.request.method}:$uri")
            val responseDigest = md5("$ha1:$nonce:$nc:$cnonce:$qop:$ha2")
            val authValue = buildString {
                append("Digest username=\"").append(user)
                append("\", realm=\"").append(realm)
                append("\", nonce=\"").append(nonce)
                append("\", uri=\"").append(uri)
                append("\", qop=$qop")
                append(", nc=$nc, cnonce=\"").append(cnonce)
                append("\", response=\"").append(responseDigest)
                params["opaque"]?.let { append("\", opaque=\"$it\"") }
                append('"')
            }
            return response.request.newBuilder()
                .header("Authorization", authValue)
                .build()
        }

        private fun responseCount(resp: Response): Int {
            var count = 1
            var prior = resp.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }

        private fun parseDigestHeader(header: String): Map<String, String> {
            val regex = Regex("(\\w+)=\"([^\"]*)\"")
            return regex.findAll(header).associate { it.groupValues[1] to it.groupValues[2] }
        }
    }

    private suspend fun OkHttpClient.awaitCall(request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (!cont.isCancelled) cont.resumeWithException(e)
                }
                override fun onResponse(call: okhttp3.Call, response: Response) {
                    cont.resume(response)
                }
            })
        }
}