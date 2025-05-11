package net.eupixel.vivlib.srbp

import java.net.ServerSocket
import java.net.Socket
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.util.Base64

class Server(private val port: Int, secret: String) {

    private val secretBytes = secret.toByteArray(Charsets.UTF_8)
    private val clients = ConcurrentHashMap<Socket, PrintWriter>()
    @Volatile private var running = false
    private lateinit var serverSocket: ServerSocket

    private fun hmacSha256(data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretBytes, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun randomNonce(length: Int = 16): ByteArray {
        val nonce = ByteArray(length)
        SecureRandom().nextBytes(nonce)
        return nonce
    }

    fun start(onMessage: (Socket, String) -> Unit) {
        running = true
        serverSocket = ServerSocket(port)
        thread {
            try {
                while (running) {
                    val client = serverSocket.accept()
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val writer = PrintWriter(client.getOutputStream(), true)
                    val nonce = randomNonce()
                    writer.println(Base64.getEncoder().encodeToString(nonce))
                    val clientHmac = Base64.getDecoder().decode(reader.readLine() ?: "")
                    if (!hmacSha256(nonce).contentEquals(clientHmac)) {
                        client.close()
                        continue
                    }
                    writer.println("OK")
                    clients[client] = writer
                    thread {
                        try {
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                onMessage(client, line!!)
                            }
                        } catch (_: IOException) {
                        } finally {
                            disconnect(client)
                        }
                    }
                }
            } catch (_: IOException) {
            }
        }
    }

    fun send(to: Socket, message: String) {
        clients[to]?.println(message)
    }

    fun broadcast(message: String) {
        clients.values.forEach { it.println(message) }
    }

    fun disconnect(client: Socket) {
        clients.remove(client)
        try { client.close() } catch (_: IOException) {}
    }

    fun stop() {
        running = false
        try { serverSocket.close() } catch (_: IOException) {}
        clients.keys.forEach { disconnect(it) }
    }
}