package net.eupixel.core

import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object Messenger {
    private data class ServerInfo(val host: String, val port: Int)
    private val targets = mutableMapOf<String, ServerInfo>()
    private val listeners = mutableMapOf<String, MutableList<(String) -> Unit>>()
    private val globalListeners = mutableListOf<(String, String) -> Unit>()
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

    fun send(targetName: String, channel: String, msg: String) {
        val info = targets[targetName] ?: return
        Socket(info.host, info.port).use { sock ->
            sock.getOutputStream().bufferedWriter().apply {
                write("$channel:$msg")
                newLine()
                flush()
            }
        }
    }

    fun broadcast(channel: String, msg: String) {
        targets.keys.forEach { send(it, channel, msg) }
    }

    fun bind(ip: String, port: Int) {
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
                            listeners[channel]?.forEach { handler -> handler(message) }
                            globalListeners.forEach { handler -> handler(channel, message) }
                        }
                    }
                }
            }
        }
    }
}
