package net.eupixel.core

import net.eupixel.model.Message
import net.eupixel.model.Translation
import java.util.Locale

object DBTranslator {
    private var keys: List<String> = emptyList()
    private val defaultLocale = Locale.US
    private val messages = mutableListOf<Message>()

    fun loadFromDB() {
        keys = DirectusClient.listItems("messages", "key")
        messages.clear()
        keys.forEach { key ->
            DirectusClient.getLocalizedMap(
                collection      = "messages",
                filterField     = "key",
                filterValue     = key,
                arrayField      = "translations"
            )?.let { localeMap ->
                val translations = localeMap.map { (localeTag, text) ->
                    val locale = toLocale(localeTag)
                    Translation(locale, text)
                }
                messages.add(Message(key, ArrayList(translations)))
            }
        }
    }

    @Suppress("DEPRECATION")
    fun toLocale(code: String): Locale {
        val parts = code.split('_')
        return when (parts.size) {
            1 -> Locale(parts[0])
            2 -> Locale(parts[0], parts[1].uppercase())
            else -> Locale.US
        }
    }

    fun translate(key: String, locale: Locale): String {
        val msg = messages.find { it.key == key }
        msg?.translations?.find { it.locale == locale }?.let {
            return it.message
        }
        msg?.translations?.find { it.locale == defaultLocale }?.let {
            return it.message
        }
        return "no translation"
    }

    fun get(key: String, locale: Locale): String {
        return translate(key, locale).replace("<prefix>", translate("prefix", defaultLocale))
    }
}