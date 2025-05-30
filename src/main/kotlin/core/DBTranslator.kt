package net.eupixel.core

import net.eupixel.model.Message
import net.eupixel.model.Translation
import java.util.Locale

class DBTranslator(
    private val keys: Array<String>,
    private val defaultLocale: Locale = Locale.US,
    private val fallbackMessage: String = "no translation"
) {
    private val messages = mutableListOf<Message>()

    init {
        loadFromDB()
    }

    fun loadFromDB() {
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
        val msg = messages.find { it.getKey() == key }
        msg?.getTranslations()?.find { it.getLocale() == locale }?.let {
            return it.getMessage()
        }
        msg?.getTranslations()?.find { it.getLocale() == defaultLocale }?.let {
            return it.getMessage()
        }
        return fallbackMessage
    }

    fun get(key: String, locale: Locale): String {
        return translate(key, locale).replace("<prefix>", translate("prefix", defaultLocale))
    }
}