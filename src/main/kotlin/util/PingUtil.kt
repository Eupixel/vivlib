package net.eupixel.vivlib.util

import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import kotlin.io.use
import kotlin.text.toByteArray

object PingUtil {
    fun ping(host: String, port: Int = 25565): Pair<Boolean, Int> {
        return try {
            Socket(host, port).use { socket ->
                socket.soTimeout = 3000
                val out = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())
                val hostBytes = host.toByteArray(StandardCharsets.UTF_8)
                val packetLength = 1 + varIntSize(47) + varIntSize(hostBytes.size) + hostBytes.size + 2 + 1
                writeVarInt(out, packetLength)
                writeVarInt(out, 0x00)
                writeVarInt(out, 47)
                writeVarInt(out, hostBytes.size)
                out.write(hostBytes)
                out.writeShort(port)
                writeVarInt(out, 1)
                writeVarInt(out, 1)
                writeVarInt(out, 0x00)
                readVarInt(input)
                readVarInt(input)
                val stringLength = readVarInt(input)
                val responseBytes = ByteArray(stringLength)
                input.readFully(responseBytes)
                val response = String(responseBytes, StandardCharsets.UTF_8)
                val json = JSONObject(response)
                val playerCount = json.optJSONObject("players")?.optInt("online", 0) ?: 0
                Pair(true, playerCount)
            }
        } catch (_: Exception) {
            Pair(false, 0)
        }
    }

    private fun writeVarInt(out: DataOutputStream, value: Int) {
        var v = value
        while (v and 0x80 != 0) {
            out.writeByte(v and 0x7F or 0x80)
            v = v ushr 7
        }
        out.writeByte(v and 0x7F)
    }

    private fun readVarInt(input: DataInputStream): Int {
        var result = 0
        var shift = 0
        while (true) {
            val b = input.readByte().toInt()
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        return result
    }

    private fun varIntSize(value: Int): Int {
        var v = value
        var size = 0
        while (v and 0x80 != 0) {
            size++
            v = v ushr 7
        }
        return size + 1
    }
}