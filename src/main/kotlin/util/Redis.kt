package net.eupixel.vivlib.util

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub
import kotlin.concurrent.thread

object RedisUtil {
    private data class Conn(val host: String, val port: Int, val jedis: Jedis)
    private val connections = mutableMapOf<String, Conn>()

    fun configure(name: String, host: String, port: Int = 6379) {
        connections[name] = Conn(host, port, Jedis(host, port))
    }

    fun addListener(name: String, channel: String, handler: (channel: String, message: String) -> Unit) {
        val (host, port, _) = connections[name] ?: error("Connection '$name' not configured")
        thread {
            Jedis(host, port).use { sub ->
                sub.subscribe(object : JedisPubSub() {
                    override fun onMessage(ch: String, msg: String) {
                        handler(ch, msg)
                    }
                }, channel)
            }
        }
    }

    fun addGlobalListener(channel: String, handler: (connectionName: String, channel: String, message: String) -> Unit) {
        connections.forEach { (name, conn) ->
            thread {
                Jedis(conn.host, conn.port).use { sub ->
                    sub.subscribe(object : JedisPubSub() {
                        override fun onMessage(ch: String, msg: String) {
                            handler(name, ch, msg)
                        }
                    }, channel)
                }
            }
        }
    }

    fun send(name: String, channel: String, message: String) {
        connections[name]?.jedis?.publish(channel, message)
            ?: error("Connection '$name' not configured")
    }
}