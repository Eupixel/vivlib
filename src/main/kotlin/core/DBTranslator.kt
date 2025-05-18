package net.eupixel.core

import net.eupixel.model.Message
import net.eupixel.model.Translation
import java.text.MessageFormat
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
                    Translation(locale, MessageFormat(text))
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

    fun translate(key: String, locale: Locale): MessageFormat {
        val msg = messages.find { it.getKey() == key }
        msg?.getTranslations()?.find { it.getLocale() == locale }?.let {
            return it.getMessageFormat()
        }
        msg?.getTranslations()?.find { it.getLocale() == defaultLocale }?.let {
            return it.getMessageFormat()
        }
        return MessageFormat(fallbackMessage)
    }

    fun get(key: String, locale: Locale): String {
        return translate(key, locale).toPattern().replace("<prefix>", translate("prefix", defaultLocale).toPattern())
    }
}