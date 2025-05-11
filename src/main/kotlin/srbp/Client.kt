package net.eupixel.vivlib.srbp
import java.net.Socket
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import kotlin.concurrent.thread
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

class Client(host: String, port: Int, secret: String, private val onReceive: (String) -> Unit) {

    private val secretBytes = secret.toByteArray(Charsets.UTF_8)
    private val socket = Socket(host, port)
    private val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    private val writer = PrintWriter(socket.getOutputStream(), true)
    private val readerThread: Thread

    private fun hmacSha256(data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretBytes, "HmacSHA256"))
        return mac.doFinal(data)
    }

    init {
        val nonce = Base64.getDecoder().decode(reader.readLine() ?: "")
        val clientHmac = hmacSha256(nonce)
        writer.println(Base64.getEncoder().encodeToString(clientHmac))
        if (reader.readLine() != "OK") throw IOException("Authentication failed")
        readerThread = thread {
            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    onReceive(line!!)
                }
            } catch (_: IOException) {
            }
        }
    }

    fun send(message: String) {
        writer.println(message)
    }

    fun disconnect() {
        try { socket.close() } catch (_: IOException) {}
        try { readerThread.join() } catch (_: InterruptedException) {}
    }
}