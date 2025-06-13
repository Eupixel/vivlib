package net.eupixel.vivlib.core

import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.network.packet.server.common.TransferPacket
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.Instant
import java.util.UUID.fromString
import kotlin.concurrent.thread

object Messenger {
    private data class ServerInfo(val host: String, val port: Int)
    private val targets = mutableMapOf<String, ServerInfo>()
    private val listeners = mutableMapOf<String, MutableList<(String) -> Unit>>()
    private val globalListeners = mutableListOf<(String, String) -> Unit>()
    private val requestHandlers = mutableMapOf<String, (String) -> String>()
    private var serverSocket: ServerSocket? = null

    fun registerTarget(name: String, host: String, port: Int) {
        targets[name] = ServerInfo(host, port)
    }

    fun unregisterTarget(name: String) {
        targets.remove(name)
    }

    fun addListener(channel: String, handler: (String) -> Unit) {
        listeners.getOrPut(channel) { mutableListOf() }.add(handler)
    }

    fun addGlobalListener(handler: (String, String) -> Unit) {
        globalListeners.add(handler)
    }

    fun addRequestHandler(channel: String, handler: (String) -> String) {
        requestHandlers[channel] = handler
    }

    fun sendRequest(targetName: String, channel: String, msg: String): String? {
        val info = targets[targetName] ?: return null
        Socket(info.host, info.port).use { sock ->
            val writer = sock.getOutputStream().bufferedWriter()
            writer.write("$channel:$msg")
            writer.newLine()
            writer.flush()
            val response = sock.getInputStream().bufferedReader().readLine()
            return response
        }
    }

    fun send(targetName: String, channel: String, msg: String) {
        Socket(targetName, 2903).use { sock ->
            val writer = sock.getOutputStream().bufferedWriter()
            writer.write("$channel:$msg")
            writer.newLine()
            writer.flush()
        }
    }

    fun broadcast(channel: String, msg: String) {
        targets.keys.forEach { send(it, channel, msg) }
    }

    fun bind(ip: String, port: Int) {
        println("Starting Messenger!")
        serverSocket = ServerSocket().apply {
            bind(InetSocketAddress(ip, port))
        }
        thread {
            serverSocket!!.use { ss ->
                while (true) {
                    val sock = ss.accept()
                    thread {
                        sock.use {
                            val line = it.getInputStream().bufferedReader().readLine()
                            val (channel, message) = line.split(":", limit = 2)
                            if (requestHandlers.containsKey(channel)) {
                                val response = requestHandlers[channel]!!.invoke(message)
                                it.getOutputStream().bufferedWriter().apply {
                                    write(response)
                                    newLine()
                                    flush()
                                }
                            }
                            listeners[channel]?.forEach { handler -> handler(message) }
                            globalListeners.forEach { handler -> handler(channel, message) }
                        }
                    }
                }
            }
        }
    }

    fun addBaseListener() {
        addListener("add_whitelist") { msg ->
            val uuid = msg.split("&")[0]
            val ttl = msg.split("&")[1].toInt()
            val timestamp = Instant.parse(msg.split("&")[2])
            WhitelistManager.add(uuid, ttl, timestamp)
        }
        addListener("transfer") { msg ->
            val uuid = msg.split("&")[0]
            val target = msg.split("&")[1]
            MinecraftServer.getConnectionManager().onlinePlayers.forEach {
                if(it.uuid.toString() == uuid) {
                    it.sendPacket(TransferPacket(target.split("&")[0], target.split("&")[1].toInt()))
                }
            }
        }
        addListener("action") { msg ->
            val uuid = msg.split("&")[0]
            val msg = msg.split("&")[1]
            MinecraftServer.getConnectionManager().onlinePlayers.forEach {
                if(it.uuid.toString() == uuid) {
                    it.sendActionBar(MiniMessage.miniMessage().deserialize(msg))
                }
            }
        }
        addListener("message") { msg ->
            val uuid = msg.split("&")[0]
            val msg = msg.split("&")[1]
            MinecraftServer.getConnectionManager().onlinePlayers.forEach {
                if(it.uuid.toString() == uuid) {
                    it.sendMessage(MiniMessage.miniMessage().deserialize(msg))
                }
            }
        }
        addRequestHandler("player_online", this::playerOnline)
    }

    fun playerOnline(uuid: String): String {
        return (MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(fromString(uuid)) != null).toString()
    }
}