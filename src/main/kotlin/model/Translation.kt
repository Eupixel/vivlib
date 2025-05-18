package net.eupixel.model

import java.text.MessageFormat
import java.util.Locale

class Translation(private val locale: Locale, private val message: MessageFormat) {
    fun getLocale(): Locale {
        return locale
    }

    fun getMessageFormat(): MessageFormat {
        return message
    }
}