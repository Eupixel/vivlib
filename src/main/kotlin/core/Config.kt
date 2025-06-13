package net.eupixel.vivlib.core

import net.eupixel.vivlib.core.DirectusClient.getData

object Config {
    var chatFormat: String = ""

    fun init() {
        chatFormat = getData("global_values", "name", "chat_format", "data").orEmpty()
    }
}